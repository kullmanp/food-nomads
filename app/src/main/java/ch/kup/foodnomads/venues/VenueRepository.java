package ch.kup.foodnomads.venues;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where Venues are kept. The seam that lets {@link Venues} run in unit tests without a database. */
public interface VenueRepository {

    void add(Venue venue);

    /** All Venues, newest first, with their proposing Member loaded. */
    List<Venue> newestFirst();

    /** The Venue, held for this transaction, so that whatever depends on it changes one at a time. */
    Optional<Venue> findForUpdate(UUID id);
}
