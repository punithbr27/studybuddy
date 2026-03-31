package main.pattern;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * DatabaseManager — implements the SINGLETON PATTERN.
 *
 * WHY SINGLETON?
 * ==============
 * Imagine 10 different parts of your app each opening their OWN database 
 * connection. That's wasteful and can cause conflicts (e.g., two parts 
 * trying to write to the same row). 
 *
 * The Singleton pattern ensures there is ONLY ONE instance of this class 
 * in the entire application. Everyone shares the same connection.
 *
 * HOW IT WORKS:
 * =============
 * 1. The constructor is PRIVATE — so nobody can do "new DatabaseManager()"
 * 2. We have a static variable 'instance' that holds the ONE object
 * 3. The static method getInstance() either creates the object (first time)
 *    or returns the existing one (every other time)
 *
 * This is called "Lazy Initialization" — the object is created only when
 * someone first asks for it, not when the program starts.
 */
public class DatabaseManager {

    // ─── Step 1: A private static variable to hold the SINGLE instance ───
    // This starts as null. The first call to getInstance() will create it.
    private static DatabaseManager instance = null;

    // The actual database connection object
    private Connection connection;

    // Path to our SQLite database file
    private static final String DB_URL = "jdbc:sqlite:study_buddy.db";

    // ─── Step 2: PRIVATE constructor — nobody outside can call this ───
    // If this was public, anyone could do "new DatabaseManager()" and 
    // create multiple instances, breaking the Singleton rule.
    private DatabaseManager() {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            // Create the connection to the database file
            this.connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] Connected to SQLite database successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] SQLite JDBC driver not found!");
            System.err.println("    Make sure sqlite-jdbc jar is in your classpath.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to connect to database!");
            e.printStackTrace();
        }
    }

    // ─── Step 3: The public static method to get the SINGLE instance ───
    // This is the ONLY way to get a DatabaseManager object.
    //
    // First call:  instance is null → creates new DatabaseManager → returns it
    // Second call: instance is NOT null → just returns the existing one
    //
    // This means the constructor runs ONLY ONCE in the entire application!
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // ─── Get the database connection ───
    // Other classes use this to run SQL queries
    public Connection getConnection() {
        return this.connection;
    }

    // ─── Initialize tables from schema.sql ───
    // Reads the SQL file and creates all tables
    public void initializeDatabase(String schemaFilePath) {
        try {
            // Read the entire schema.sql file
            StringBuilder sql = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(schemaFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip comment lines (lines starting with --)
                if (!line.trim().startsWith("--")) {
                    sql.append(line).append("\n");
                }
            }
            reader.close();

            // Split by semicolons to get individual SQL statements
            String[] statements = sql.toString().split(";");

            // Execute each CREATE TABLE statement
            Statement stmt = connection.createStatement();
            for (String s : statements) {
                if (!s.trim().isEmpty()) {
                    stmt.execute(s.trim());
                }
            }
            stmt.close();

            System.out.println("[DB] All tables created successfully!");

        } catch (IOException e) {
            System.err.println("[DB ERROR] Could not read schema file: " + schemaFilePath);
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to create tables!");
            e.printStackTrace();
        }
    }

    // ─── Close the connection (cleanup) ───
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
