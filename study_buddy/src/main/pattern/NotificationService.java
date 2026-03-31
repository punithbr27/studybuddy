package main.pattern;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NotificationService — implements Observer Pattern
 * 
 * Stores notifications in the database and notifies all registered observers.
 * When a session status changes, this service is called to notify users.
 */
public class NotificationService implements Observer {

    private List<Observer> observers = new ArrayList<>();

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void update(String eventType, String message, int userId) {
        // Save notification to database
        saveNotification(userId, message);
        // Notify all other observers
        for (Observer observer : observers) {
            if (observer != this) {
                observer.update(eventType, message, userId);
            }
        }
    }

    public void notifySessionUpdate(int userId, String sessionStatus, int sessionId) {
        String message = "";
        switch (sessionStatus) {
            case "PENDING":
                message = "New booking request! Session #" + sessionId + " is waiting for your response.";
                break;
            case "CONFIRMED":
                message = "Session #" + sessionId + " has been confirmed! 🎉";
                break;
            case "CANCELLED":
                message = "Session #" + sessionId + " has been cancelled.";
                break;
            case "COMPLETED":
                message = "Session #" + sessionId + " is completed. Please leave a rating! ⭐";
                break;
        }
        update(sessionStatus, message, userId);
    }

    private void saveNotification(int userId, String message) {
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Notification save error: " + e.getMessage());
        }
    }

    public List<String[]> getNotifications(int userId) {
        List<String[]> notifications = new ArrayList<>();
        String sql = "SELECT message, is_read, created_at FROM notifications " +
                "WHERE user_id = ? ORDER BY created_at DESC LIMIT 20";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notifications.add(new String[] {
                        rs.getString("message"),
                        rs.getInt("is_read") == 0 ? "unread" : "read",
                        rs.getString("created_at")
                });
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error fetching notifications: " + e.getMessage());
        }
        return notifications;
    }

    public int getUnreadCount(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            int count = rs.getInt(1);
            rs.close();
            pstmt.close();
            return count;
        } catch (SQLException e) {
            return 0;
        }
    }

    public void markAllRead(int userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error marking notifications read: " + e.getMessage());
        }
    }
}
