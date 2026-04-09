package main.pattern;

import main.model.Session;
import java.sql.*;

/**
 * CreateBookingCommand — Concrete Command for creating a session booking.
 * Implements execute() to create and undo() to cancel.
 */
public class CreateBookingCommand implements BookingCommand {

    private Session session;
    private int createdSessionId = -1;

    public CreateBookingCommand(Session session) {
        this.session = session;
    }

    @Override
    public boolean execute() {
        // Check for conflicts first
        if (hasConflict()) {
            System.out.println("❌ Booking conflict! Tutor already has a session at this time.");
            return false;
        }

        String sql = "INSERT INTO sessions (tutor_id, student_id, subject_id, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING')";

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, session.getTutorId());
            pstmt.setInt(2, session.getStudentId());
            pstmt.setInt(3, session.getSubjectId());
            pstmt.setString(4, session.getStartTime());
            pstmt.setString(5, session.getEndTime());
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                createdSessionId = keys.getInt(1);
                session.setSessionId(createdSessionId);
            }
            keys.close();
            pstmt.close();

            System.out.println("✅ Booking created! Session ID: " + createdSessionId);
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Booking error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean undo() {
        if (createdSessionId == -1)
            return false;

        String sql = "UPDATE sessions SET status = 'CANCELLED' WHERE session_id = ?";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, createdSessionId);
            pstmt.executeUpdate();
            pstmt.close();
            System.out.println("↩️ Booking " + createdSessionId + " cancelled (undo).");
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Undo error: " + e.getMessage());
            return false;
        }
    }

    private boolean hasConflict() {
        String sql = "SELECT COUNT(*) FROM sessions WHERE tutor_id = ? " +
                "AND status IN ('PENDING', 'CONFIRMED') " +
                "AND start_time = ? AND end_time = ?";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, session.getTutorId());
            pstmt.setString(2, session.getStartTime());
            pstmt.setString(3, session.getEndTime());
            ResultSet rs = pstmt.executeQuery();
            boolean conflict = rs.getInt(1) > 0;
            rs.close();
            pstmt.close();
            return conflict;
        } catch (SQLException e) {
            return false;
        }
    }

    public int getCreatedSessionId() {
        return createdSessionId;
    }
}
