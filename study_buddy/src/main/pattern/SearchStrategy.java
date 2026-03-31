package main.pattern;

import main.model.Tutor;
import java.util.List;

/**
 * SearchStrategy — STRATEGY PATTERN (Interface)
 *
 * WHAT IS THE STRATEGY PATTERN?
 * =============================
 * The Strategy Pattern lets you define a FAMILY of algorithms (search methods),
 * put each one in its own class, and make them SWAPPABLE at runtime.
 *
 * WHY USE IT?
 * ===========
 * Our app has 3 ways to search for tutors:
 * 1. By Rating → Show highest-rated tutors first
 * 2. By Subject → Find tutors who teach a specific subject
 * 3. By Availability → Show only tutors who are currently available
 *
 * WITHOUT Strategy Pattern:
 * You'd have ONE giant method with if-else:
 * if (filter == "rating") { ... }
 * else if (filter == "subject") { ... }
 * else if (filter == "availability") { ... }
 * 
 * Adding a new filter means editing this method → messy!
 *
 * WITH Strategy Pattern:
 * Each filter is its OWN class that implements this interface.
 * Adding a new filter = just create a new class. Nothing else changes.
 *
 * HOW IT WORKS:
 * 1. This INTERFACE defines "what" (search method signature)
 * 2. Each concrete class defines "how" (the actual algorithm)
 * 3. The SearchController holds a reference to this interface
 * 4. At runtime, you SWAP which strategy is being used
 *
 * DIAGRAM:
 * ┌──────────────────┐
 * │ SearchStrategy │ ← Interface
 * │ + search() │
 * └───────┬──────────┘
 * ┌────────────┼────────────────┐
 * ▼ ▼ ▼
 * SearchByRating SearchBySubject SearchByAvailability
 */
public interface SearchStrategy {

    /**
     * Search for tutors using this strategy's algorithm.
     * 
     * @param keyword The search term (subject name, code, etc.)
     * @return List of matching Tutor objects
     */
    List<Tutor> search(String keyword);
}
