package ch.kup.foodnomads.venues;

/** There is no Venue with this id. */
public class VenueNotFoundException extends RuntimeException {

    public VenueNotFoundException() {
        super("Venue not found");
    }
}
