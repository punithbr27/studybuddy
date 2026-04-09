package main.pattern;

/**
 * BookingCommand — COMMAND PATTERN (Interface)
 *
 * The Command Pattern encapsulates an action as an object.
 * This lets us: execute, undo, and log booking operations.
 *
 * Each command has:
 * - execute() → Do the action
 * - undo() → Reverse the action
 */
public interface BookingCommand {
    boolean execute();

    boolean undo();
}
