package ch.kup.foodnomads.members;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where Passkeys are kept (ADR-0005). */
public interface PasskeyRepository {

    void add(Passkey passkey);

    Optional<Passkey> find(String credentialId);

    List<Passkey> ofMember(UUID memberId);
}
