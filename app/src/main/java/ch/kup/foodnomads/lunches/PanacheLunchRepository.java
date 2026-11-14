package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Keeps Lunches, their comments, their Candidate Venues, their Votes and their RSVPs in PostgreSQL.
 * Callers run it inside a transaction.
 */
@ApplicationScoped
class PanacheLunchRepository implements LunchRepository {

    private static final String ONE_PLANNED = "lunch_one_planned";

    @Override
    public void add(Lunch lunch) {
        try {
            lunch.persistAndFlush();
        } catch (PersistenceException e) {
            if (violatesOnePlanned(e)) {
                throw new LunchAlreadyPlannedException();
            }
            throw e;
        }
    }

    private static boolean violatesOnePlanned(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException violation
                    && ONE_PLANNED.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Lunch> find(UUID id) {
        Optional<Lunch> lunch = Lunch.<Lunch>find(
                        "from Lunch l left join fetch l.venue left join fetch l.comments c left join fetch c.author"
                                + " where l.id = ?1",
                        id)
                .singleResultOptional();
        // More queries, so that comments, Candidate Venues, Votes and RSVPs don't multiply each other.
        // They fill in the same Lunch and Candidate Venues, which this transaction already holds.
        lunch.ifPresent(l -> {
            Lunch.find("from Lunch l left join fetch l.candidates c left join fetch c.venue where l.id = ?1", id)
                    .singleResult();
            CandidateVenue.find("from CandidateVenue c left join fetch c.votes where c.lunch.id = ?1", id)
                    .list();
            Lunch.find("from Lunch l left join fetch l.rsvps r left join fetch r.member where l.id = ?1", id)
                    .singleResult();
        });
        return lunch;
    }

    @Override
    public Optional<Lunch> findForUpdate(UUID id) {
        return Lunch.find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    @Override
    public Optional<Lunch> planned() {
        // There is at most one, and a collection fetch can't be limited to the first row in SQL.
        return Lunch.find(
                        "from Lunch l left join fetch l.venue left join fetch l.rsvps where l.state = ?1",
                        LunchState.PLANNED)
                .singleResultOptional();
    }

    @Override
    public List<Lunch> all() {
        return Lunch.find("from Lunch l left join fetch l.venue left join fetch l.rsvps")
                .list();
    }

    @Override
    public void addComment(LunchComment comment) {
        comment.author = Member.getEntityManager().merge(comment.author);
        comment.persist();
    }

    @Override
    public void addCandidate(CandidateVenue candidate) {
        candidate.persist();
    }

    @Override
    public void removeCandidate(CandidateVenue candidate) {
        candidate.delete();
    }

    @Override
    public void addVote(Vote vote) {
        vote.member = Member.getEntityManager().merge(vote.member);
        vote.persist();
    }

    @Override
    public void removeVote(Vote vote) {
        vote.delete();
    }

    @Override
    public void addRsvp(Rsvp rsvp) {
        rsvp.member = Member.getEntityManager().merge(rsvp.member);
        rsvp.persist();
    }

    @Override
    public void removeRsvp(Rsvp rsvp) {
        rsvp.delete();
    }
}
