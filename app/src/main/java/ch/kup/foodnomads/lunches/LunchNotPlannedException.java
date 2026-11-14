package ch.kup.foodnomads.lunches;

/** The Lunch is held or cancelled, so it can't change any more. */
public class LunchNotPlannedException extends RuntimeException {

    public LunchNotPlannedException() {
        super("Lunch not planned");
    }
}
