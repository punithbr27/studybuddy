package main.pattern;

import java.sql.*;

/**
 * CancelBookingCommand — Concrete Command for cancelling a session.
 */
public class CancelBookingCommand implements BookingCommand {

    private int sessionId;
    private String previousStatus = null;

    public CancelBookingCommand(int sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public boolean execute() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // Save previous status for undo
            PreparedStatement query = conn.prepareStatement(
                    "SELECT status FROM sessions WHERE session_id = ?");
            query.setInt(1, sessionId);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                previousStatus = rs.getString("status");
            }
            rs.close();
            query.close();

            // Cancel the session
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE sessions SET status = 'CANCELLED' WHERE session_id = ?");
            pstmt.setInt(1, sessionId);
            int rows = pstmt.executeUpdate();
            pstmt.close();

            if (rows > 0) {
                System.out.println("✅ Session " + sessionId + " cancelled.");
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("❌ Cancel error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean undo() {
        if (previousStatus == null)
            return false;

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(
                    "UPDATE sessions SET status = ? WHERE session_id = ?");
            pstmt.setString(1, previousStatus);
            pstmt.setInt(2, sessionId);
            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("↩️ Session " + sessionId + " restored to " + previousStatus);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Undo error: " + e.getMessage());
            return false;
        }
    }
}
