package com.tunesphere;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String URL = "jdbc:sqlite:tunesphere.db"; // local database file

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
            System.out.println("✅ Connected to SQLite!");
            createUsersTable(conn);
        } catch (SQLException e) {
            System.out.println("❌ SQLite connection failed!");
            e.printStackTrace();
        }
        return conn;
    }

    private static void createUsersTable(Connection conn) {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            );
        """;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("✅ Users table ready!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
