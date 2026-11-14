package ch.kup.foodnomads.lunches;

/** The Planned Lunch has picked this Venue, so a past Lunch can't be recorded there. */
public class VenuePickedException extends RuntimeException {

    public VenuePickedException() {
        super("Venue picked");
    }
}
