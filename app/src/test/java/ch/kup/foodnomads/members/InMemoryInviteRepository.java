package ch.kup.foodnomads.members;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An {@link InviteRepository} for unit tests. */
class InMemoryInviteRepository implements InviteRepository {

    private final Map<String, Invite> invites = new HashMap<>();

    @Override
    public void add(Invite invite) {
        invites.put(invite.token, invite);
    }

    @Override
    public Optional<Invite> find(String token) {
        return Optional.ofNullable(invites.get(token));
    }

    @Override
    public Optional<Invite> findForUpdate(String token) {
        return find(token);
    }
}
