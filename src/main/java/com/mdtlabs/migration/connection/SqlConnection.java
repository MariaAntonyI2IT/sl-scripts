package com.mdtlabs.migration.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlConnection {
    
    public static String connectionURL;
    public static String username;
    public static String password;

    private Connection connection = null;

    private static SqlConnection sqlConnection = null; 

    private SqlConnection() {
        try {
            System.out.println("try connecting....");
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(connectionURL, username, password);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.print(e);    
        }
    }

    public static SqlConnection getSqlConnection() {
        if (null == sqlConnection) {
            sqlConnection = new SqlConnection();
        } 
        return sqlConnection;
    }

    public Connection getConnection() {
        return connection;
    }
}
