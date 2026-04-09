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
 * or returns the existing one (every other time)
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

    // Database URL — reads from environment variable or falls back to SQLite
    private static final String DB_URL;
    private static final boolean isPostgres;
    private static final String DB_USER;
    private static final String DB_PASSWORD;

    static {
        String envHost = System.getenv("DB_HOST");
        if (envHost != null && !envHost.isEmpty()) {
            // Supabase/PostgreSQL mode
            String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : "5432";
            String dbName = System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "postgres";
            DB_URL = "jdbc:postgresql://" + envHost + ":" + port + "/" + dbName;
            DB_USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "postgres";
            DB_PASSWORD = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";
            isPostgres = true;
        } else {
            // Local SQLite mode
            DB_URL = "jdbc:sqlite:study_buddy.db";
            DB_USER = null;
            DB_PASSWORD = null;
            isPostgres = false;
        }
    }

    // ─── Step 2: PRIVATE constructor — nobody outside can call this ───
    private DatabaseManager() {
        try {
            if (isPostgres) {
                Class.forName("org.postgresql.Driver");
                java.util.Properties props = new java.util.Properties();
                props.setProperty("user", DB_USER);
                props.setProperty("password", DB_PASSWORD);
                props.setProperty("sslmode", "require");
                this.connection = DriverManager.getConnection(DB_URL, props);
                System.out.println("[DB] Connected to PostgreSQL (Supabase) successfully!");
            } else {
                Class.forName("org.sqlite.JDBC");
                this.connection = DriverManager.getConnection(DB_URL);
                System.out.println("[DB] Connected to SQLite database successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DB ERROR] JDBC driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Failed to connect to database!");
            System.err.println("[DB ERROR] URL: " + DB_URL);
            e.printStackTrace();
        }
    }

    public static boolean isPostgresMode() {
        return isPostgres;
    }

    // ─── Step 3: The public static method to get the SINGLE instance ───
    // This is the ONLY way to get a DatabaseManager object.
    //
    // First call: instance is null → creates new DatabaseManager → returns it
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
        // Pick the correct schema file based on database mode
        if (isPostgres) {
            schemaFilePath = schemaFilePath.replace("schema.sql", "schema_postgres.sql");
        }

        try {
            // Read the entire schema file
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
