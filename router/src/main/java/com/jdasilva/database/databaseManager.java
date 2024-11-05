package com.jdasilva.database;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class databaseManager {

    private static final Dotenv dotenv = Dotenv.configure().directory(".").load();
    private static final String URL = "jdbc:postgresql://localhost:5432/";
    private static final String DB_NAME =  dotenv.get("POSTGRES_DB");
    private static final String USER = dotenv.get("POSTGRES_USER");
    private static final String PASSWORD = dotenv.get("POSTGRES_PASSWORD");

    public databaseManager() {
        createDatabase();
        createdTables(DB_NAME);
    }

    private static void createDatabase() {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = DriverManager.getConnection(URL + "postgres", USER, PASSWORD);
            statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'");
            if(!resultSet.next()) {
                statement.executeUpdate("CREATE DATABASE " + DB_NAME);
                System.out.println("Database " + DB_NAME +" created successfully...");
            } else {
                System.out.println("Database " + DB_NAME + " already exists...");
            }

            System.out.println("Database created successfully...");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createdTables(String dbName)
    {
        Connection connection = null;
        Statement statement = null;

        try {
            connection = DriverManager.getConnection(URL + DB_NAME, USER, PASSWORD);
            statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                "id SERIAL PRIMARY KEY," + 
                "client_id INT NOT NULL," +
                "type VARCHAR(50) NOT NULL," + 
                "message VARCHAR(255) NOT NULL)");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    statement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getURL() {
        return URL;
    }

    public static String getDB_NAME() {
        return DB_NAME;
    }

    public static String getUSER() {
        return USER;
    }

    public static String getPASSWORD() {
        return PASSWORD;
    }

}
