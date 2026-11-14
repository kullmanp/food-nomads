package ch.kup.foodnomads.lunches;

/** A Lunch from before the app took place before today in Basel, not today or later. */
public class DateNotPastException extends RuntimeException {

    public DateNotPastException() {
        super("Date not past");
    }
}
