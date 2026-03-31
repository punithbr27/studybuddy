package main.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Tutor — extends STUDENT (not User directly!)
 *
 * WHY DOES TUTOR EXTEND STUDENT?
 * ===============================
 * From your synopsis: "A Tutor IS a Student who also teaches."
 * A tutor still has a semester, CGPA, and enrolled courses (they're a student
 * too!).
 * Tutor just ADDS teaching-specific stuff on top:
 * - subjects they teach
 * - reputation score (based on ratings)
 * - availability status
 *
 * INHERITANCE CHAIN:
 * User (abstract)
 * └── Student (has semester, cgpa)
 * └── Tutor (has reputation, subjects they teach)
 *
 * So a Tutor object has ALL attributes from User + Student + Tutor!
 */
public class Tutor extends Student {

    // ─── Tutor-Specific Attributes ───
    private double reputationScore; // Average rating (1.0 to 5.0)
    private int totalRatings; // How many ratings received
    private boolean isAvailable; // Currently accepting bookings?
    private List<String> teachingSubjects; // Subject codes they can teach

    // ─── Constructor ───
    public Tutor(int userId, String name, String email, String password,
            int semester, double cgpa) {
        super(userId, name, email, password, semester, cgpa);
        this.role = "TUTOR"; // Override the role set by Student
        this.reputationScore = 0.0;
        this.totalRatings = 0;
        this.isAvailable = true;
        this.teachingSubjects = new ArrayList<>();
    }

    // Constructor for new tutors (no userId yet)
    public Tutor(String name, String email, String password,
            int semester, double cgpa) {
        super(name, email, password, semester, cgpa);
        this.role = "TUTOR";
        this.reputationScore = 0.0;
        this.totalRatings = 0;
        this.isAvailable = true;
        this.teachingSubjects = new ArrayList<>();
    }

    // ─── Override displayProfile — Tutor has MORE info to show ───
    @Override
    public String displayProfile() {
        return "╔══════════════════════════════╗\n" +
                "║       TUTOR PROFILE          ║\n" +
                "╠══════════════════════════════╣\n" +
                "  Name       : " + name + "\n" +
                "  Email      : " + email + "\n" +
                "  Semester   : " + getSemester() + "\n" +
                "  CGPA       : " + getCgpa() + "\n" +
                "  Rating     : " + String.format("%.1f", reputationScore) +
                " ⭐ (" + totalRatings + " reviews)\n" +
                "  Available  : " + (isAvailable ? "Yes ✅" : "No ❌") + "\n" +
                "  Teaches    : " + teachingSubjects + "\n" +
                "╚══════════════════════════════╝";
    }

    // ─── Tutor-Specific Methods ───

    // Add a subject this tutor can teach
    public void addTeachingSubject(String subjectCode) {
        if (!teachingSubjects.contains(subjectCode)) {
            teachingSubjects.add(subjectCode);
        }
    }

    // Update reputation when a new rating comes in
    // Formula: newAverage = (oldAverage * count + newRating) / (count + 1)
    public void updateReputation(int newRating) {
        double totalScore = reputationScore * totalRatings + newRating;
        totalRatings++;
        reputationScore = totalScore / totalRatings;
    }

    // ─── Getters & Setters ───
    public double getReputationScore() {
        return reputationScore;
    }

    public int getTotalRatings() {
        return totalRatings;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public List<String> getTeachingSubjects() {
        return teachingSubjects;
    }

    public void setReputationScore(double score) {
        this.reputationScore = score;
    }

    public void setTotalRatings(int count) {
        this.totalRatings = count;
    }

    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }
}
