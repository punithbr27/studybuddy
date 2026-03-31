package main;

import main.pattern.DatabaseManager;
import main.server.StudyBuddyServer;

/**
 * Main.java — Entry point of Study Buddy.
 * Initializes the database and starts the web server.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Study Buddy — Starting Up 📚     ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();

        // Step 1: Initialize database (Singleton Pattern)
        DatabaseManager db = DatabaseManager.getInstance();
        db.initializeDatabase("src/sql/schema.sql");
        System.out.println();

        // Step 8: Start the web server
        try {
            int port = 8080;
            StudyBuddyServer server = new StudyBuddyServer(port);
            server.start();
            System.out.println();
            System.out.println("╔══════════════════════════════════════╗");
            System.out.println("║  Open: http://localhost:" + port + "          ║");
            System.out.println("║  Press Ctrl+C to stop the server     ║");
            System.out.println("╚══════════════════════════════════════╝");
        } catch (Exception e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
