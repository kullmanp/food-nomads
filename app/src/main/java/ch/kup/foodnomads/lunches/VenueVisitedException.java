package ch.kup.foodnomads.lunches;

/** A Lunch has been held at this Venue already, and the group never goes back. */
public class VenueVisitedException extends RuntimeException {

    public VenueVisitedException() {
        super("Venue visited");
    }
}
