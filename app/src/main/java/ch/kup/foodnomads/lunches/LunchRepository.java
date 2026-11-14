package ch.kup.foodnomads.lunches;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where Lunches, their comments and their Candidate Venues are kept (ADR-0005). */
public interface LunchRepository {

    /**
     * Stores a new Lunch. Fails with {@link LunchAlreadyPlannedException} if it is planned and
     * another Lunch was planned in the meantime.
     */
    void add(Lunch lunch);

    /** The Lunch with its Venue, comments and their authors, and Candidate Venues with their Venues loaded. */
    Optional<Lunch> find(UUID id);

    /** Like {@link #find}, but holds the Lunch for this transaction, so that its state changes one at a time. */
    Optional<Lunch> findForUpdate(UUID id);

    Optional<Lunch> planned();

    /** All Lunches with their Venues, in no particular order. */
    List<Lunch> all();

    void addComment(LunchComment comment);

    void addCandidate(CandidateVenue candidate);

    void removeCandidate(CandidateVenue candidate);
}
