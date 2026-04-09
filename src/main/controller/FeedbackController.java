package main.controller;

import main.model.Feedback;
import main.pattern.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FeedbackController — handles ratings and reputation scores
 */
public class FeedbackController {

    private DatabaseManager dbManager;

    public FeedbackController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // Submit feedback for a completed session
    public Feedback submitFeedback(int sessionId, int studentId, int tutorId,
            int rating, String comment) {
        // Check if session is completed
        String checkSql = "SELECT status FROM sessions WHERE session_id = ?";
        try {
            PreparedStatement check = dbManager.getConnection().prepareStatement(checkSql);
            check.setInt(1, sessionId);
            ResultSet rs = check.executeQuery();
            if (!rs.next() || !rs.getString("status").equals("COMPLETED")) {
                System.out.println("❌ Can only rate completed sessions!");
                rs.close();
                check.close();
                return null;
            }
            rs.close();
            check.close();
        } catch (SQLException e) {
            System.err.println("❌ Check error: " + e.getMessage());
            return null;
        }

        String sql = "INSERT INTO feedback (session_id, student_id, tutor_id, rating, comment) " +
                "VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, sessionId);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, tutorId);
            pstmt.setInt(4, rating);
            pstmt.setString(5, comment);
            pstmt.executeUpdate();

            Feedback feedback = new Feedback(sessionId, studentId, tutorId, rating, comment);
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                feedback.setFeedbackId(keys.getInt(1));
            }
            keys.close();
            pstmt.close();

            System.out.println("✅ Feedback submitted! Rating: " + rating + " ⭐");
            return feedback;

        } catch (SQLException e) {
            System.err.println("❌ Feedback error: " + e.getMessage());
            return null;
        }
    }

    // Get average rating for a tutor
    public double getTutorRating(int tutorId) {
        String sql = "SELECT COALESCE(AVG(rating), 0) as avg_rating FROM feedback WHERE tutor_id = ?";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, tutorId);
            ResultSet rs = pstmt.executeQuery();
            double rating = rs.getDouble("avg_rating");
            rs.close();
            pstmt.close();
            return rating;
        } catch (SQLException e) {
            return 0;
        }
    }

    // Get all feedback for a tutor
    public List<Feedback> getFeedbackForTutor(int tutorId) {
        List<Feedback> feedbackList = new ArrayList<>();
        String sql = "SELECT * FROM feedback WHERE tutor_id = ? ORDER BY created_at DESC";
        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, tutorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                feedbackList.add(new Feedback(
                        rs.getInt("feedback_id"),
                        rs.getInt("session_id"),
                        rs.getInt("student_id"),
                        rs.getInt("tutor_id"),
                        rs.getInt("rating"),
                        rs.getString("comment")));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }
        return feedbackList;
    }
}
