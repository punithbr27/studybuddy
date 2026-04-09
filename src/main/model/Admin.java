package main.model;

/**
 * Admin — represents a system administrator who can verify accounts
 */
public class Admin extends User {

    public Admin(int userId, String name, String email, String password, boolean isVerified) {
        super(userId, name, email, password, "ADMIN", isVerified);
    }

    public Admin(String name, String email, String password) {
        super(-1, name, email, password, "ADMIN", true); // Admins are auto-verified
    }

    @Override
    public String displayProfile() {
        return "Admin Profile: " + name + " (" + email + ")";
    }
}
