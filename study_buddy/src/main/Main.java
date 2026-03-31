package main;

import main.pattern.DatabaseManager;
import main.pattern.UserFactory;
import main.controller.AuthController;
import main.model.User;

/**
 * Main.java — Entry point of the Study Buddy application.
 *
 * Currently demos:
 * Step 1: Singleton Pattern (DatabaseManager)
 * Step 2: Factory Pattern (UserFactory) + Registration/Login
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Welcome to Study Buddy! 📚       ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println();

        // ─── STEP 1 DEMO: Singleton Pattern ────────────
        System.out.println("═══ STEP 1: Singleton Pattern Demo ═══");
        DatabaseManager db1 = DatabaseManager.getInstance();
        DatabaseManager db2 = DatabaseManager.getInstance();
        System.out.println("db1 = " + db1);
        System.out.println("db2 = " + db2);
        System.out.println(db1 == db2 ? "✅ Same object — Singleton works!" : "❌ Broken!");
        System.out.println();

        // Initialize database tables
        db1.initializeDatabase("src/sql/schema.sql");
        System.out.println();

        // ─── STEP 2 DEMO: Factory Pattern + Auth ────────
        System.out.println("═══ STEP 2: Factory Pattern Demo ═══");
        System.out.println();

        AuthController auth = new AuthController();

        // Register a Student using the Factory (behind the scenes)
        System.out.println("--- Registering a Student ---");
        User student = auth.register("STUDENT", "Punith BR", "punith@pes.edu", "pass123", 6, 8.5);
        if (student != null) {
            System.out.println(student.displayProfile());
        }
        System.out.println();

        // Register a Tutor
        System.out.println("--- Registering a Tutor ---");
        User tutor = auth.register("TUTOR", "Jay Kariya", "jay@pes.edu", "pass456", 6, 9.0);
        if (tutor != null) {
            System.out.println(tutor.displayProfile());
        }
        System.out.println();

        // Try duplicate email — should fail
        System.out.println("--- Trying duplicate email ---");
        auth.register("STUDENT", "Duplicate", "punith@pes.edu", "xyz", 4, 7.0);
        System.out.println();

        // Login test
        System.out.println("--- Login Test ---");
        User loggedIn = auth.login("punith@pes.edu", "pass123");
        if (loggedIn != null) {
            System.out.println("Logged in as: " + loggedIn);
        }
        System.out.println();

        // Wrong password test
        System.out.println("--- Wrong Password Test ---");
        auth.login("punith@pes.edu", "wrongpassword");
        System.out.println();

        System.out.println("Step 2 complete! User model + Factory + Auth ready. 🎉");

        // Clean up
        db1.closeConnection();
    }
}
