
package com.campus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconn {

    private static final String URL = System.getenv()
            .getOrDefault("DB_URL",
                    "jdbc:mysql://localhost:3306/campus_skill_exchange");

    private static final String USER = System.getenv()
            .getOrDefault("DB_USER", "root");

    private static final String PASSWORD =
            System.getenv().getOrDefault("DB_PASSWORD", "");

    public static Connection getConnection() throws SQLException {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver could not be loaded.", e);
        }

        if (PASSWORD.isBlank()) {
            throw new SQLException(
                    "Please set the DB_PASSWORD environment variable.");
        }

        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection con = getConnection()) {
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
