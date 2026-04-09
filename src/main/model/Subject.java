package main.model;

/**
 * Subject — represents an academic subject/course
 *
 * WHAT IS THIS?
 * =============
 * This is a simple model class (also called a "data class" or "entity").
 * It mirrors the 'subjects' table in our database.
 *
 * Each subject has:
 * - subjectCode (e.g., "CS304")
 * - name (e.g., "Object-Oriented Analysis & Design")
 * - semester (e.g., 6)
 *
 * RELATIONSHIP:
 * A Tutor can teach MANY subjects.
 * A Subject can have MANY tutors.
 * This is a MANY-TO-MANY relationship, handled by the 'tutor_subjects' table.
 */
public class Subject {

    private int subjectId;
    private String subjectCode;
    private String name;
    private int semester;

    // ─── Full Constructor (when loading from database) ───
    public Subject(int subjectId, String subjectCode, String name, int semester) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.name = name;
        this.semester = semester;
    }

    // ─── Constructor for new subjects (no ID yet) ───
    public Subject(String subjectCode, String name, int semester) {
        this(-1, subjectCode, name, semester);
    }

    // ─── Getters ───
    public int getSubjectId() {
        return subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getName() {
        return name;
    }

    public int getSemester() {
        return semester;
    }

    // ─── Setters ───
    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    @Override
    public String toString() {
        return subjectCode + " - " + name + " (Sem " + semester + ")";
    }
}
