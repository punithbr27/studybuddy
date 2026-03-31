package main.pattern;

import main.model.User;
import main.model.Student;
import main.model.Tutor;
import main.model.Admin;

/**
 * UserFactory — implements the FACTORY METHOD PATTERN
 *
 * WHAT IS THE FACTORY PATTERN?
 * ============================
 * A Factory is a class whose ONLY JOB is to CREATE objects.
 * Instead of writing "new Student(...)" or "new Tutor(...)" scattered
 * throughout your code, you call ONE method:
 *
 * UserFactory.createUser("STUDENT", name, email, ...)
 *
 * The Factory decides which class to instantiate based on the 'role' parameter.
 *
 * WHY USE IT?
 * ===========
 * 1. Centralized: All object creation logic is in ONE place
 * 2. Easy to extend: Want to add "ADMIN" role? Just add one more case here
 * 3. Decoupled: The rest of your code doesn't need to know about
 * Student/Tutor classes — it just works with the 'User' type
 *
 * REAL-WORLD ANALOGY:
 * Think of a car factory. You say "I want an SUV" → the factory builds it.
 * You don't need to know HOW it's built, you just get the car.
 */
public class UserFactory {

    /**
     * Creates and returns the appropriate User subclass based on the role.
     *
     * Notice the return type is 'User' (the parent class), not Student or Tutor.
     * This is called POLYMORPHISM — we can treat any Student/Tutor as a User.
     *
     * @param role     "STUDENT" or "TUTOR"
     * @param name     User's full name
     * @param email    University email
     * @param password User's password
     * @param semester Current semester (1-8)
     * @param cgpa     Current CGPA
     * @return A User object (which is actually a Student or Tutor underneath)
     */
    public static User createUser(String role, String name, String email,
            String password, int semester, double cgpa) {
        return createUser(role, name, email, password, semester, cgpa, false);
    }

    public static User createUser(String role, String name, String email,
            String password, int semester, double cgpa, boolean isVerified) {
        switch (role.toUpperCase()) {
            case "STUDENT":
                return new Student(-1, name, email, password, semester, cgpa, isVerified);
            case "TUTOR":
                return new Tutor(-1, name, email, password, semester, cgpa, isVerified);
            case "ADMIN":
                return new Admin(-1, name, email, password, isVerified);
            default:
                throw new IllegalArgumentException(
                        "[Factory ERROR] Unknown role: '" + role + "'. " +
                                "Valid roles are: STUDENT, TUTOR, ADMIN");
        }
    }
}
