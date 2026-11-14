package ch.kup.foodnomads.lunches;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where Lunches, their comments, their Candidate Venues, their Votes and their RSVPs are kept (ADR-0005). */
public interface LunchRepository {

    /**
     * Stores a new Lunch. Fails with {@link LunchAlreadyPlannedException} if it is planned and
     * another Lunch was planned in the meantime.
     */
    void add(Lunch lunch);

    /**
     * The Lunch with its Venue, comments and their authors, Candidate Venues with their Venues and
     * Votes, and RSVPs with their Members loaded.
     */
    Optional<Lunch> find(UUID id);

    /** Like {@link #find}, but holds the Lunch for this transaction, so that its state changes one at a time. */
    Optional<Lunch> findForUpdate(UUID id);

    /** The Planned Lunch with its RSVPs, but not their Members. */
    Optional<Lunch> planned();

    /** All Lunches with their Venues and RSVPs, but not the RSVPs' Members, in no particular order. */
    List<Lunch> all();

    void addComment(LunchComment comment);

    void addCandidate(CandidateVenue candidate);

    /** Removes the Candidate Venue. Its Votes must be removed first. */
    void removeCandidate(CandidateVenue candidate);

    void addVote(Vote vote);

    void removeVote(Vote vote);

    void addRsvp(Rsvp rsvp);

    void removeRsvp(Rsvp rsvp);
}
