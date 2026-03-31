package main.pattern;

/**
 * Observer — OBSERVER PATTERN (Interface)
 *
 * Any class that wants to receive notifications implements this.
 * When a session status changes, all registered observers get notified.
 */
public interface Observer {
    void update(String eventType, String message, int userId);
}
