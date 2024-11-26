package com.mdtlabs.migration.script;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.mdtlabs.migration.connection.SqlConnection;
import com.mdtlabs.migration.model.Constants;

public class FacilityReportAdmin {
    private Connection connection =
            SqlConnection.getSqlConnection().getConnection();
    int count = 0;

    public void updateReportUserOrganization() throws SQLException {
        List<Integer> userIds = new ArrayList<>();
        List<Integer> organizationIds = new ArrayList<>();
        Statement statement = connection.createStatement();

        ResultSet resultSet = statement.executeQuery(Constants.FACILITY_REPORT_ADMIN_QUERY);
        while (resultSet.next()) {
            userIds.add(resultSet.getInt(Constants.ID));
            organizationIds.add(resultSet.getInt(Constants.ORGANIZATION_ID));
        }

        for (int i = 0; i < userIds.size(); i++) {
            int userId = userIds.get(i);
            int organizationId = organizationIds.get(i);

            count++;
            System.out.println("userId : " + userId + " | organizationId : " + organizationId + " | count : " + count);

            String insertQuery = String.format(Constants.INSERT_INTO_REPORT_USER_ORGANIZATION, userId, organizationId);
            statement.executeUpdate(insertQuery);
        }

        // Close the statement
        statement.close();
    }
}
