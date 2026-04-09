package main.controller;

import main.model.Tutor;
import main.pattern.SearchStrategy;
import main.pattern.SearchByRating;
import main.pattern.SearchBySubject;
import main.pattern.SearchByAvailability;

import java.util.List;

/**
 * SearchController — The CONTEXT class in Strategy Pattern
 *
 * WHAT IS A CONTEXT?
 * ==================
 * In the Strategy Pattern, the "Context" is the class that USES the strategy.
 * It holds a reference to a SearchStrategy interface and delegates the
 * search work to whatever concrete strategy is currently set.
 *
 * THE MAGIC:
 * The SearchController doesn't know or care WHETHER it's using
 * SearchByRating, SearchBySubject, or SearchByAvailability.
 * It just calls strategy.search() — and the right algorithm runs!
 *
 * This is POLYMORPHISM in action:
 * - The variable type is SearchStrategy (interface)
 * - The actual object is SearchByRating (concrete class)
 * - Java calls the correct search() method automatically
 *
 * SWAPPING AT RUNTIME:
 * controller.setStrategy(new SearchByRating()); ← Now sorts by rating
 * controller.setStrategy(new SearchBySubject()); ← Now sorts by subject
 * controller.setStrategy(new SearchByAvailability()); ← Now shows available
 * only
 *
 * The same controller, different behavior — just by swapping the strategy!
 */
public class SearchController {

    // ─── The current strategy (can be swapped at runtime) ───
    private SearchStrategy strategy;

    // Default: search by subject
    public SearchController() {
        this.strategy = new SearchBySubject();
    }

    // ─── Set/swap the strategy ───
    // This is how you change the search algorithm at runtime!
    public void setStrategy(SearchStrategy strategy) {
        this.strategy = strategy;
        System.out.println("[Search] Strategy set to: " + strategy.getClass().getSimpleName());
    }

    // ─── Execute the search using the current strategy ───
    // The controller DELEGATES the work to the strategy object.
    // It doesn't know HOW the search works — that's the strategy's job.
    public List<Tutor> search(String keyword) {
        return strategy.search(keyword);
    }

    // ─── Convenience methods — set strategy + search in one call ───
    public List<Tutor> searchByRating(String keyword) {
        setStrategy(new SearchByRating());
        return search(keyword);
    }

    public List<Tutor> searchBySubject(String keyword) {
        setStrategy(new SearchBySubject());
        return search(keyword);
    }

    public List<Tutor> searchByAvailability(String keyword) {
        setStrategy(new SearchByAvailability());
        return search(keyword);
    }
}
