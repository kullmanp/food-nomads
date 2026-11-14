package ch.kup.foodnomads.lunches;

/** The Lunch's date is still ahead, so it can't be held yet. */
public class LunchNotDueException extends RuntimeException {

    public LunchNotDueException() {
        super("Lunch not due");
    }
}
