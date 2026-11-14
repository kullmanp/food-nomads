package ch.kup.foodnomads.lunches;

/** The Lunch has no Venue, so it can't be held. */
public class NoVenueException extends RuntimeException {

    public NoVenueException() {
        super("No Venue");
    }
}
