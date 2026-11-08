package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.Optional;

/** Keeps Invites in PostgreSQL. Callers run it inside a transaction. */
@ApplicationScoped
class PanacheInviteRepository implements InviteRepository {

    @Override
    public void add(Invite invite) {
        invite.persist();
    }

    @Override
    public Optional<Invite> find(String token) {
        return Invite.find("token", token).firstResultOptional();
    }

    @Override
    public Optional<Invite> findForUpdate(String token) {
        return Invite.find("token", token)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }
}
