package main.model;

/**
 * Session — a booked study session between a Student and a Tutor
 */
public class Session {

    private int sessionId;
    private int tutorId;
    private int studentId;
    private int subjectId;
    private String startTime;
    private String endTime;
    private String status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    // Extra fields for display (not in DB directly)
    private String tutorName;
    private String studentName;
    private String subjectName;

    public Session(int sessionId, int tutorId, int studentId, int subjectId,
            String startTime, String endTime, String status) {
        this.sessionId = sessionId;
        this.tutorId = tutorId;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Session(int tutorId, int studentId, int subjectId,
            String startTime, String endTime) {
        this(-1, tutorId, studentId, subjectId, startTime, endTime, "PENDING");
    }

    // Getters
    public int getSessionId() {
        return sessionId;
    }

    public int getTutorId() {
        return tutorId;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getTutorName() {
        return tutorName;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    // Setters
    public void setSessionId(int id) {
        this.sessionId = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTutorName(String name) {
        this.tutorName = name;
    }

    public void setStudentName(String name) {
        this.studentName = name;
    }

    public void setSubjectName(String name) {
        this.subjectName = name;
    }

    @Override
    public String toString() {
        return "Session #" + sessionId + " [" + status + "] Tutor:" + tutorId +
                " Student:" + studentId + " " + startTime + " - " + endTime;
    }
}
