package ch.kup.foodnomads.lunches;

/** A Lunch is planned already, and there is at most one at a time. */
public class LunchAlreadyPlannedException extends RuntimeException {

    public LunchAlreadyPlannedException() {
        super("Lunch already planned");
    }
}
