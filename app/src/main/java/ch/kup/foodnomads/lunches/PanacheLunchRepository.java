package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

/** Keeps Lunches and their comments in PostgreSQL. Callers run it inside a transaction. */
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
        return Lunch.find(
                        "from Lunch l left join fetch l.venue left join fetch l.comments c left join fetch c.author"
                                + " where l.id = ?1",
                        id)
                .singleResultOptional();
    }

    @Override
    public Optional<Lunch> findForUpdate(UUID id) {
        return Lunch.find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    @Override
    public Optional<Lunch> planned() {
        return Lunch.find("state", LunchState.PLANNED).firstResultOptional();
    }

    @Override
    public List<Lunch> all() {
        return Lunch.find("from Lunch l left join fetch l.venue").list();
    }

    @Override
    public void addComment(LunchComment comment) {
        comment.author = Member.getEntityManager().merge(comment.author);
        comment.persist();
    }
}
