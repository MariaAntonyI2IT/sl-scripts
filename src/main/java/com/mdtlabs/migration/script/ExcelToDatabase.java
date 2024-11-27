package com.mdtlabs.migration.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdtlabs.migration.model.Constants;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.postgresql.util.PGobject;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Class handel the update of Meta values to the Database.
 *
 * @author premkalyan
 * @since 27/11/2024
 */
public class ExcelToDatabase {

    public static String EXCEL_FILEPATH;
    public static String CONFIG_FILEPATH;
    public static String CONNECTION_URL;
    public static String USER_NAME;
    public static String PASSWORD;
    public static int BUNDLE_SIZE;

    public void updatedMetaInDatabase() {
        try (Connection connection = DriverManager.getConnection(CONNECTION_URL, USER_NAME, PASSWORD);
             FileInputStream fileInputStream = new FileInputStream(EXCEL_FILEPATH);
             Workbook workbook = new XSSFWorkbook(fileInputStream)) {

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode config = objectMapper.readTree(Files.readAllBytes(Paths.get(CONFIG_FILEPATH)));
            JsonNode tablesConfig = config.get(Constants.TABLES);

            // Iterate through each sheet in the workbook
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String tableName = sheet.getSheetName();

                // Check if the table name matches any entry in the configuration file
                if (tableExistsInConfig(tablesConfig, tableName)) {
                    // If it matches, truncate the table if it exists
                    if (tableExists(connection, tableName)) {
                        truncateTable(connection, tableName);
                    }
                    // Insert data from the sheet into the database
                    insertData(connection, sheet, tablesConfig);
                } else {
                    System.out.println("Skipping sheet '" + tableName + "' as it does not match any configured table.");
                }
            }
        } catch (IOException | SQLException e) {
            System.out.println("Error processing Excel file or database operation: " + e.getMessage());
        }
    }

    private static boolean tableExistsInConfig(JsonNode tablesConfig, String tableName) {
        for (JsonNode table : tablesConfig) {
            if (table.get(Constants.NAME).asText().equalsIgnoreCase(tableName)) {
                return true; // Table name matches
            }
        }
        return false; // No match found
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(Constants.SELECT_QUERY)) {
            statement.setString(1, tableName.toLowerCase());
            ResultSet rs = statement.executeQuery();
            return rs.next() && rs.getBoolean(1);
        }
    }

    private static void truncateTable(Connection connection, String tableName) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("TRUNCATE TABLE " + tableName + " RESTART IDENTITY CASCADE");
            System.out.println("Truncated table: " + tableName);
        }
    }

    //Get columns from the config file and add default audit columns
    private static List<String> getConfiguredColumns(JsonNode tableConfig) {
        List<String> columnNames = new ArrayList<>();
        for (JsonNode column : tableConfig.get(Constants.COLUMNS)) {
            String columnName = column.get(Constants.NAME).asText();
            columnNames.add(columnName);
        }
        addNewColumns(columnNames);
        return columnNames;
    }

    // Construct insert query for the given columns
    private static StringBuilder buildInsertSQL(Sheet sheet, List<String> columnNames) {
        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + sheet.getSheetName() + " (");

        for (String header : columnNames) {
            insertSQL.append(header).append(", ");
        }
        insertSQL.setLength(insertSQL.length() - 2); // Remove last comma and space
        insertSQL.append(") VALUES (");

        for (int i = 0; i < columnNames.size(); i++) {
            insertSQL.append("?");
            if (i < columnNames.size() - 1) {
                insertSQL.append(", ");
            }
        }
        return insertSQL.append(")");
    }

    //Validate to the condition and process the insertion of data to the columns
    private static void insertData(Connection connection, Sheet sheet, JsonNode tablesConfig) throws SQLException {
        JsonNode tableConfig = getTableConfig(tablesConfig, sheet.getSheetName());
        if (tableConfig == null) {
            System.out.println("No configuration found for table: " + sheet.getSheetName());
            return;
        }
        List<String> columnNames = getConfiguredColumns(tableConfig);
        StringBuilder insertSQL = buildInsertSQL(sheet, columnNames);

        try (PreparedStatement statement = connection.prepareStatement(insertSQL.toString())) {
            int batchSize = 0;

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || !hasValidData(row)) continue;

                int paramIndex = 1;

                for (int colIndex = 0; colIndex < columnNames.size(); colIndex++) {

                    if (colIndex < tableConfig.get(Constants.COLUMNS).size()) {
                        JsonNode columnConfig = tableConfig.get(Constants.COLUMNS).get(colIndex);
                        String columnType = columnConfig.get(Constants.TYPE).asText();
                        Cell cell = row.getCell(colIndex);

                        // Handle foreign key relationship validation
                        if (isForeignKeyRelationship(tableConfig, columnConfig, connection)) {
                            paramIndex = setForeignKeyParameter(paramIndex, cell, connection, tableConfig, statement);
                        } else {
                            paramIndex = setColumnValue(paramIndex, cell, columnType, statement, connection);
                        }
                    } else {
                        paramIndex = setDefaultValues(paramIndex, columnNames.get(colIndex), statement);
                    }
                }
                statement.addBatch();
                batchSize++;

                if (batchSize % Constants.BATCH_SIZE == Constants.ZERO) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch(); // Execute any remaining inserts in the batch
        } catch (SQLException e) {
            System.out.println("Error processing while inserting data: " + e.getMessage());
        }
    }

    private static JsonNode getTableConfig(JsonNode tablesConfig, String tableName) {
        for (JsonNode table : tablesConfig) {
            if (table.get(Constants.NAME).asText().equalsIgnoreCase(tableName)) {
                return table; // Return the configuration node for the matching table name
            }
        }
        return null; // No matching configuration found
    }

    private static boolean hasValidData(Row row) {
        for (int i = 0; i < row.getPhysicalNumberOfCells(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return true;
            }
        }
        return false;
    }

    private static void addNewColumns(List<String> headers) {
        headers.add(Constants.CREATED_BY);
        headers.add(Constants.UPDATED_BY);
        headers.add(Constants.CREATED_AT);
        headers.add(Constants.UPDATED_AT);
        headers.add(Constants.IS_ACTIVE);
        headers.add(Constants.IS_DELETED);
    }

    // Foreign Key Relation for relation tables if exists
    private static boolean isForeignKeyRelationship(JsonNode tableConfig, JsonNode columnConfig, Connection connection) throws SQLException {
        return !tableConfig.get(Constants.RELATIONSHIP).isEmpty() &&
                tableExists(connection, tableConfig.get(Constants.RELATIONSHIP).get(0).get(Constants.TARGET_TABLE).asText()) &&
                tableConfig.get(Constants.RELATIONSHIP).get(0).get(Constants.FOREIGN_KEY).asText().equalsIgnoreCase(columnConfig.get(Constants.NAME).asText());
    }

    private static int setForeignKeyParameter(int paramIndex, Cell cell, Connection connection, JsonNode tableConfig, PreparedStatement statement) throws SQLException {
        String query = "SELECT EXISTS (SELECT 1 FROM " +
                tableConfig.get(Constants.RELATIONSHIP).get(0).get(Constants.TARGET_TABLE) + " WHERE id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, (int) cell.getNumericCellValue());
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getBoolean(1)) {
                statement.setInt(paramIndex++, (int) cell.getNumericCellValue());
            }
        }
        return paramIndex;
    }

    private static int setColumnValue(int paramIndex, Cell cell, String columnType, PreparedStatement statement, Connection connection) throws SQLException {
        if (cell == null) {
            statement.setObject(paramIndex++, null);
            return paramIndex;
        }

        switch (columnType.toLowerCase()) {
            case Constants.VARCHAR:
                statement.setString(paramIndex++, getStringCellValue(cell));
                break;
            case Constants.INT:
                statement.setInt(paramIndex++, (int) cell.getNumericCellValue());
                break;
            case Constants.DOUBLE:
                statement.setDouble(paramIndex++, cell.getNumericCellValue());
                break;
            case Constants.BOOLEAN:
                statement.setBoolean(paramIndex++, cell.getBooleanCellValue());
                break;
            case Constants.JSONB:
                setJsonbParameter(paramIndex++, cell, statement);
                break;
            case Constants.VARCHAR_ARRAY:
                statement.setArray(paramIndex++, connection.createArrayOf(Constants.VARCHAR, new String[]{cell.getStringCellValue()}));
                break;
            default:
                statement.setObject(paramIndex++, null);
        }

        return paramIndex;
    }

    private static String getStringCellValue(Cell cell) {
        return cell.getCellType() == CellType.STRING ?
                cell.getStringCellValue() :
                String.valueOf(cell.getNumericCellValue());
    }

    private static void setJsonbParameter(int paramIndex, Cell cell, PreparedStatement statement) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType(Constants.JSONB);

        if (cell != null && cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()) {
            jsonObject.setValue(cell.getStringCellValue().trim());
        } else {
            jsonObject.setValue("{}");
        }
        statement.setObject(paramIndex++, jsonObject);
    }

    private static int setDefaultValues(int paramIndex, String columnName, PreparedStatement statement) throws SQLException {
        switch (columnName) {
            case Constants.CREATED_BY:
            case Constants.UPDATED_BY:
                statement.setInt(paramIndex++, 1); // Example: User ID 1
                break;
            case Constants.CREATED_AT:
            case Constants.UPDATED_AT:
                statement.setTimestamp(paramIndex++, Timestamp.valueOf(java.time.LocalDateTime.now()));
                break;
            case Constants.IS_ACTIVE:
                statement.setBoolean(paramIndex++, true);
                break;
            case Constants.IS_DELETED:
                statement.setBoolean(paramIndex++, false);
                break;
            default:
                statement.setObject(paramIndex++, null); // Handle other cases
        }
        return paramIndex;
    }
}
