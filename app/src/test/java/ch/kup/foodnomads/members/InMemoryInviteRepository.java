package ch.kup.foodnomads.members;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<Invite> findForUpdate(UUID id) {
        return invites.values().stream().filter(invite -> invite.id.equals(id)).findFirst();
    }

    @Override
    public List<Invite> newestFirst() {
        return invites.values().stream()
                .sorted(Comparator.comparing((Invite i) -> i.createdAt)
                        .reversed()
                        .thenComparing(i -> i.id))
                .toList();
    }
}
