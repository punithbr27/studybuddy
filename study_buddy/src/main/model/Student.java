package main.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Student — CONCRETE CLASS extending User
 *
 * WHAT DOES 'extends' MEAN?
 * =========================
 * "Student extends User" means Student INHERITS everything from User:
 * - All attributes (userId, name, email, password, role)
 * - All methods (getters, setters, toString)
 *
 * Student then ADDS its own specific attributes:
 * - semester, cgpa, enrolledCourses
 *
 * Think of it like this:
 * User = "I have a name, email, password"
 * Student = "I have everything a User has, PLUS a semester and CGPA"
 */
public class Student extends User {

    // ─── Student-Specific Attributes ───
    private int semester;
    private double cgpa;
    private List<String> enrolledCourses; // List of course codes

    // ─── Constructor ───
    // 'super(...)' calls the PARENT class (User) constructor
    // This sets up the common fields (name, email, etc.)
    public Student(int userId, String name, String email, String password,
            int semester, double cgpa, boolean isVerified) {
        super(userId, name, email, password, "STUDENT", isVerified);
        this.semester = semester;
        this.cgpa = cgpa;
        this.enrolledCourses = new ArrayList<>();
    }

    public Student(String name, String email, String password, int semester, double cgpa) {
        this(-1, name, email, password, semester, cgpa, false);
    }

    // ─── Implementing the abstract method from User ───
    // We MUST implement this, otherwise Java will give a compile error.
    @Override
    public String displayProfile() {
        return "╔══════════════════════════════╗\n" +
                "║      STUDENT PROFILE         ║\n" +
                "╠══════════════════════════════╣\n" +
                "  Name     : " + name + "\n" +
                "  Email    : " + email + "\n" +
                "  Semester : " + semester + "\n" +
                "  CGPA     : " + cgpa + "\n" +
                "  Courses  : " + enrolledCourses + "\n" +
                "╚══════════════════════════════╝";
    }

    // ─── Student-Specific Methods ───
    public void enrollCourse(String courseCode) {
        if (!enrolledCourses.contains(courseCode)) {
            enrolledCourses.add(courseCode);
        }
    }

    // ─── Getters & Setters ───
    public int getSemester() {
        return semester;
    }

    public double getCgpa() {
        return cgpa;
    }

    public List<String> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public void setCgpa(double cgpa) {
        this.cgpa = cgpa;
    }
}
