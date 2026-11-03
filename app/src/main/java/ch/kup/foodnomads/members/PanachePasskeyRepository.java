package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Keeps Passkeys in PostgreSQL. Callers run it inside a transaction. */
@ApplicationScoped
class PanachePasskeyRepository implements PasskeyRepository {

    @Override
    public void add(Passkey passkey) {
        passkey.persist();
    }

    @Override
    public Optional<Passkey> find(String credentialId) {
        return Passkey.findByIdOptional(credentialId);
    }

    @Override
    public List<Passkey> ofMember(UUID memberId) {
        return Passkey.list("member.id", memberId);
    }
}
