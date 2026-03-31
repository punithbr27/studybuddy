package main.pattern;

import main.model.Tutor;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchByRating — Concrete Strategy #1
 *
 * Finds tutors and sorts them by reputation score (highest first).
 * When a student wants the BEST tutors, they use this strategy.
 */
public class SearchByRating implements SearchStrategy {

    @Override
    public List<Tutor> search(String keyword) {
        List<Tutor> results = new ArrayList<>();

        // SQL: Find tutors who teach a subject matching the keyword,
        // ordered by their average feedback rating (highest first)
        String sql = "SELECT u.*, " +
                "COALESCE(AVG(f.rating), 0) as avg_rating, " +
                "COUNT(f.rating) as total_ratings " +
                "FROM users u " +
                "JOIN tutor_subjects ts ON u.user_id = ts.tutor_id " +
                "JOIN subjects s ON ts.subject_id = s.subject_id " +
                "LEFT JOIN feedback f ON u.user_id = f.tutor_id " +
                "WHERE u.role = 'TUTOR' " +
                "AND (s.name LIKE ? OR s.subject_code LIKE ?) " +
                "GROUP BY u.user_id " +
                "ORDER BY avg_rating DESC";

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Tutor tutor = buildTutorFromResultSet(rs);
                tutor.setReputationScore(rs.getDouble("avg_rating"));
                tutor.setTotalRatings(rs.getInt("total_ratings"));
                results.add(tutor);
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            System.err.println("❌ SearchByRating error: " + e.getMessage());
        }

        System.out.println("[Strategy: SearchByRating] Found " + results.size() + " tutors");
        return results;
    }

    private Tutor buildTutorFromResultSet(ResultSet rs) throws SQLException {
        return new Tutor(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getInt("semester"),
                rs.getDouble("cgpa"),
                rs.getInt("verified") == 1);
    }
}
