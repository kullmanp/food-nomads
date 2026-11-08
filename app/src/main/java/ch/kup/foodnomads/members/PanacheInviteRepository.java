package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Invite> findForUpdate(UUID id) {
        return Invite.find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }

    @Override
    public List<Invite> newestFirst() {
        return Invite.find("from Invite i join fetch i.createdBy order by i.createdAt desc, i.id")
                .list();
    }
}
