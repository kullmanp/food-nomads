package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;

/** Keeps Members in PostgreSQL. Callers run it inside a transaction. */
@ApplicationScoped
class PanacheMemberRepository implements MemberRepository {

    private static final String UNIQUE_DISPLAY_NAME = "member_display_name_lower_key";

    @Override
    public void add(Member member) {
        try {
            member.persistAndFlush();
        } catch (PersistenceException e) {
            if (violatesUniqueDisplayName(e)) {
                throw new DisplayNameTakenException();
            }
            throw e;
        }
    }

    private static boolean violatesUniqueDisplayName(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException violation
                    && UNIQUE_DISPLAY_NAME.equals(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Member> find(UUID id) {
        return Member.findByIdOptional(id);
    }

    @Override
    public boolean existsWithLowercaseDisplayName(String lowercaseName) {
        return Member.count("lower(displayName) = ?1", lowercaseName) > 0;
    }

    @Override
    public long count() {
        return Member.count();
    }
}
