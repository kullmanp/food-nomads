package ch.kup.foodnomads.venues;

import java.util.List;

/** Where Venues are kept. The seam that lets {@link Venues} run in unit tests without a database. */
public interface VenueRepository {

    void add(Venue venue);

    /** All Venues, newest first, with their proposing Member loaded. */
    List<Venue> newestFirst();
}
