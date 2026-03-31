package main.model;

/**
 * Feedback — rating given by a student to a tutor after a session
 */
public class Feedback {

    private int feedbackId;
    private int sessionId;
    private int studentId;
    private int tutorId;
    private int rating; // 1-5 stars
    private String comment;

    public Feedback(int feedbackId, int sessionId, int studentId,
            int tutorId, int rating, String comment) {
        this.feedbackId = feedbackId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.rating = rating;
        this.comment = comment;
    }

    public Feedback(int sessionId, int studentId, int tutorId,
            int rating, String comment) {
        this(-1, sessionId, studentId, tutorId, rating, comment);
    }

    // Getters
    public int getFeedbackId() {
        return feedbackId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getTutorId() {
        return tutorId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    // Setters
    public void setFeedbackId(int id) {
        this.feedbackId = id;
    }
}
