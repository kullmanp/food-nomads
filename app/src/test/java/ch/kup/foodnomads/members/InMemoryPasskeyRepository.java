package ch.kup.foodnomads.members;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A {@link PasskeyRepository} for unit tests. */
class InMemoryPasskeyRepository implements PasskeyRepository {

    private final Map<String, Passkey> passkeys = new LinkedHashMap<>();

    @Override
    public void add(Passkey passkey) {
        passkeys.put(passkey.credentialId, passkey);
    }

    @Override
    public Optional<Passkey> find(String credentialId) {
        return Optional.ofNullable(passkeys.get(credentialId));
    }

    @Override
    public List<Passkey> ofMember(UUID memberId) {
        return passkeys.values().stream().filter(p -> p.member.id.equals(memberId)).toList();
    }
}
