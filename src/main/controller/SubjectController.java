package main.controller;

import main.model.Subject;
import main.pattern.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SubjectController — manages subjects and tutor-subject links
 *
 * WHAT DOES THIS DO?
 * ==================
 * 1. Add new subjects to the system
 * 2. Get all subjects / subjects by semester
 * 3. Link a tutor to a subject (says "this tutor can teach this subject")
 * 4. Get all subjects a specific tutor teaches
 *
 * This handles the MANY-TO-MANY relationship:
 * - The 'tutor_subjects' junction table connects tutors ↔ subjects
 * - One tutor can teach MANY subjects
 * - One subject can have MANY tutors
 */
public class SubjectController {

    private DatabaseManager dbManager;

    public SubjectController() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // ═══════════════════════════════════════════
    // ADD A NEW SUBJECT
    // ═══════════════════════════════════════════
    public Subject addSubject(String subjectCode, String name, int semester) {
        String sql = "INSERT INTO subjects (subject_code, name, semester) VALUES (?, ?, ?)";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, subjectCode);
            pstmt.setString(2, name);
            pstmt.setInt(3, semester);
            pstmt.executeUpdate();

            ResultSet keys = pstmt.getGeneratedKeys();
            Subject subject = new Subject(subjectCode, name, semester);
            if (keys.next()) {
                subject.setSubjectId(keys.getInt(1));
            }
            keys.close();
            pstmt.close();

            System.out.println("✅ Subject added: " + subject);
            return subject;

        } catch (SQLException e) {
            System.err.println("❌ Error adding subject: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════
    // GET ALL SUBJECTS
    // ═══════════════════════════════════════════
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM subjects ORDER BY semester, subject_code";

        try {
            Statement stmt = dbManager.getConnection().createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("name"),
                        rs.getInt("semester")));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error fetching subjects: " + e.getMessage());
        }
        return subjects;
    }

    // ═══════════════════════════════════════════
    // GET SUBJECTS BY SEMESTER
    // ═══════════════════════════════════════════
    public List<Subject> getSubjectsBySemester(int semester) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM subjects WHERE semester = ?";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, semester);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("name"),
                        rs.getInt("semester")));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error fetching subjects: " + e.getMessage());
        }
        return subjects;
    }

    // ═══════════════════════════════════════════
    // LINK TUTOR TO SUBJECT
    // ═══════════════════════════════════════════
    /**
     * This inserts a row into the 'tutor_subjects' junction table.
     * It says: "Tutor with ID X can teach Subject with ID Y"
     *
     * JUNCTION TABLE EXPLAINED:
     * ─────────────────────────
     * tutor_subjects table:
     * | tutor_id | subject_id |
     * | 2 | 1 | ← Tutor 2 teaches Subject 1
     * | 2 | 3 | ← Tutor 2 also teaches Subject 3
     * | 5 | 1 | ← Tutor 5 also teaches Subject 1
     */
    public boolean linkTutorToSubject(int tutorId, int subjectId) {
        String sql = "INSERT OR IGNORE INTO tutor_subjects (tutor_id, subject_id) VALUES (?, ?)";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, tutorId);
            pstmt.setInt(2, subjectId);
            int rows = pstmt.executeUpdate();
            pstmt.close();

            if (rows > 0) {
                System.out.println("✅ Tutor " + tutorId + " linked to Subject " + subjectId);
                return true;
            } else {
                System.out.println("ℹ️  Tutor already linked to this subject.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error linking tutor to subject: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════
    // GET ALL SUBJECTS A TUTOR TEACHES
    // ═══════════════════════════════════════════
    /**
     * Uses a JOIN query to combine tutor_subjects + subjects tables.
     *
     * SQL JOIN EXPLAINED:
     * The tutor_subjects table only has IDs (tutor_id, subject_id).
     * To get the subject NAMES, we JOIN it with the subjects table
     * where subject_id matches.
     */
    public List<Subject> getSubjectsByTutor(int tutorId) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.* FROM subjects s " +
                "JOIN tutor_subjects ts ON s.subject_id = ts.subject_id " +
                "WHERE ts.tutor_id = ?";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setInt(1, tutorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                subjects.add(new Subject(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("name"),
                        rs.getInt("semester")));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error fetching tutor subjects: " + e.getMessage());
        }
        return subjects;
    }

    // ═══════════════════════════════════════════
    // GET SUBJECT BY CODE
    // ═══════════════════════════════════════════
    public Subject getSubjectByCode(String code) {
        String sql = "SELECT * FROM subjects WHERE subject_code = ?";

        try {
            PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql);
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Subject(
                        rs.getInt("subject_id"),
                        rs.getString("subject_code"),
                        rs.getString("name"),
                        rs.getInt("semester"));
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            System.err.println("❌ Error fetching subject: " + e.getMessage());
        }
        return null;
    }
}
