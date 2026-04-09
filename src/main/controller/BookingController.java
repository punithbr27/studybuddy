package main.controller;

import main.model.Session;
import main.pattern.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BookingController — manages session bookings using Command Pattern
 */
public class BookingController {

    private DatabaseManager dbManager;
    private NotificationService notificationService;
    private List<BookingCommand> commandHistory; // For undo support

    public BookingController() {
        this.dbManager = DatabaseManager.getInstance();
        this.notificationService = new NotificationService();
        this.commandHistory = new ArrayList<>();
    }

    // Create a new booking
    public Session createBooking(int tutorId, int studentId, int subjectId,
            String startTime, String endTime) {
        Session session = new Session(tutorId, studentId, subjectId, startTime, endTime);
        CreateBookingCommand command = new CreateBookingCommand(session);

        if (command.execute()) {
            commandHistory.add(command);
            // Observer: notify tutor about new booking
            notificationService.notifySessionUpdate(tutorId, "PENDING", session.getSessionId());
            return session;
        }
        return null;
    }

    // Accept a booking (tutor confirms)
    public boolean acceptBooking(int sessionId) {
        String sql = "UPDATE sessions SET status = 'CONFIRMED' WHERE session_id = ? AND status = 'PENDING'";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            int rows = pstmt.executeUpdate();
            pstmt.close();

            if (rows > 0) {
                // Get session details for notification
                Session session = getSessionById(sessionId);
                if (session != null) {
                    notificationService.notifySessionUpdate(session.getStudentId(), "CONFIRMED", sessionId);
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Accept error: " + e.getMessage());
        }
        return false;
    }

    // Reject/Cancel a booking
    public boolean cancelBooking(int sessionId) {
        CancelBookingCommand command = new CancelBookingCommand(sessionId);
        if (command.execute()) {
            commandHistory.add(command);
            Session session = getSessionById(sessionId);
            if (session != null) {
                notificationService.notifySessionUpdate(session.getStudentId(), "CANCELLED", sessionId);
                notificationService.notifySessionUpdate(session.getTutorId(), "CANCELLED", sessionId);
            }
            return true;
        }
        return false;
    }

    // Complete a session
    public boolean completeSession(int sessionId) {
        String sql = "UPDATE sessions SET status = 'COMPLETED' WHERE session_id = ? AND status = 'CONFIRMED'";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            int rows = pstmt.executeUpdate();
            pstmt.close();
            if (rows > 0) {
                Session session = getSessionById(sessionId);
                if (session != null) {
                    notificationService.notifySessionUpdate(session.getStudentId(), "COMPLETED", sessionId);
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Complete error: " + e.getMessage());
        }
        return false;
    }

    // Undo last command
    public boolean undoLastCommand() {
        if (commandHistory.isEmpty())
            return false;
        BookingCommand lastCommand = commandHistory.remove(commandHistory.size() - 1);
        return lastCommand.undo();
    }

    // Get session by ID
    public Session getSessionById(int sessionId) {
        String sql = "SELECT s.*, u1.name as tutor_name, u2.name as student_name, sub.name as subject_name " +
                "FROM sessions s " +
                "JOIN users u1 ON s.tutor_id = u1.user_id " +
                "JOIN users u2 ON s.student_id = u2.user_id " +
                "JOIN subjects sub ON s.subject_id = sub.subject_id " +
                "WHERE s.session_id = ?";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Session session = buildSessionFromResultSet(rs);
                rs.close();
                pstmt.close();
                return session;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        return null;
    }

    // Get all sessions for a user (as student or tutor)
    public List<Session> getSessionsForUser(int userId) {
        List<Session> sessions = new ArrayList<>();
        String sql = "SELECT s.*, u1.name as tutor_name, u2.name as student_name, sub.name as subject_name " +
                "FROM sessions s " +
                "JOIN users u1 ON s.tutor_id = u1.user_id " +
                "JOIN users u2 ON s.student_id = u2.user_id " +
                "JOIN subjects sub ON s.subject_id = sub.subject_id " +
                "WHERE s.student_id = ? OR s.tutor_id = ? " +
                "ORDER BY s.created_at DESC";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                sessions.add(buildSessionFromResultSet(rs));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        return sessions;
    }

    private Session buildSessionFromResultSet(ResultSet rs) throws SQLException {
        Session session = new Session(
                rs.getInt("session_id"),
                rs.getInt("tutor_id"),
                rs.getInt("student_id"),
                rs.getInt("subject_id"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("status"));
        try {
            session.setTutorName(rs.getString("tutor_name"));
            session.setStudentName(rs.getString("student_name"));
            session.setSubjectName(rs.getString("subject_name"));
        } catch (SQLException e) {
            // columns might not exist in all queries
        }
        return session;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
