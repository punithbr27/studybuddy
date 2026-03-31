package main.controller;

import main.model.User;
import main.model.Student;
import main.model.Tutor;
import main.pattern.DatabaseManager;
import main.pattern.UserFactory;

import java.sql.*;

/**
 * AuthController — handles REGISTRATION and LOGIN
 *
 * WHAT IS A CONTROLLER? (MVC Architecture)
 * =========================================
 * In MVC (Model-View-Controller):
 * - Model = Data classes (User, Student, Tutor)
 * - View = What the user sees (CLI menus — we'll build later)
 * - Controller = The BRAIN — connects Model and View, contains business logic
 *
 * This controller handles:
 * 1. Registering a new user (saves to database)
 * 2. Logging in (checks email + password)
 * 3. Fetching user by ID
 */
public class AuthController {

    private DatabaseManager dbManager;

    public AuthController() {
        // Get the Singleton database instance
        this.dbManager = DatabaseManager.getInstance();
    }

    // ═══════════════════════════════════════════
    // REGISTER — Create a new user account
    // ═══════════════════════════════════════════
    /**
     * Steps:
     * 1. Use UserFactory to create the right type of User object
     * 2. Insert it into the 'users' table in the database
     * 3. Return the created User with its new database ID
     */
    public User register(String role, String name, String email,
            String password, int semester, double cgpa) {

        // Step 1: Check if email already exists
        if (emailExists(email)) {
            System.out.println("❌ Registration failed: Email '" + email + "' is already registered!");
            return null;
        }

        // Step 2: Use the Factory to create the User object
        User user = UserFactory.createUser(role, name, email, password, semester, cgpa);

        // Step 3: Save to database
        String sql = "INSERT INTO users (name, email, password, role, semester, cgpa) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            // PreparedStatement prevents SQL injection attacks
            // The '?' placeholders are filled in safely below
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getEmail());
            pstmt.setString(3, user.getPassword()); // TODO: Hash password later
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, semester);
            pstmt.setDouble(6, cgpa);

            pstmt.executeUpdate();

            // Get the auto-generated user_id from the database
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                user.setUserId(keys.getInt(1));
            }
            keys.close();
            pstmt.close();

            System.out.println("✅ Registration successful! User ID: " + user.getUserId());
            return user;

        } catch (SQLException e) {
            System.err.println("❌ Registration failed: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════
    // LOGIN — Verify email + password
    // ═══════════════════════════════════════════
    public User login(String email, String password) {

        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Found a matching user! Build the object from DB data
                User user = buildUserFromResultSet(rs);
                System.out.println("✅ Login successful! Welcome, " + user.getName());
                return user;
            } else {
                System.out.println("❌ Login failed: Invalid email or password.");
                return null;
            }

        } catch (SQLException e) {
            System.err.println("❌ Login error: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════
    // GET USER BY ID
    // ═══════════════════════════════════════════
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return buildUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching user: " + e.getMessage());
        }
        return null;
    }

    // ═══════════════════════════════════════════
    // HELPER METHODS (private — internal use only)
    // ═══════════════════════════════════════════

    // Check if an email is already registered
    private boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // Build a User object from a database row
    // Uses the Factory pattern again — role from DB decides the type
    private User buildUserFromResultSet(ResultSet rs) throws SQLException {
        String role = rs.getString("role");
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        int semester = rs.getInt("semester");
        double cgpa = rs.getDouble("cgpa");

        // Use Factory to create the right type, then set the ID
        User user = UserFactory.createUser(role, name, email, password, semester, cgpa);
        user.setUserId(id);
        return user;
    }
}
