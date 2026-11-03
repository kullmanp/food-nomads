package ch.kup.foodnomads.members;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnUserProvider;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.UUID;

/** Lets {@code quarkus-security-webauthn} look up passkeys. The WebAuthn username is the Member id. */
@Blocking
@ApplicationScoped
public class MemberPasskeys implements WebAuthnUserProvider {

    private final PasskeyRepository passkeys;

    public MemberPasskeys(PasskeyRepository passkeys) {
        this.passkeys = passkeys;
    }

    @Override
    @Transactional
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String memberId) {
        UUID id = parse(memberId);
        if (id == null) {
            return Uni.createFrom().item(List.of());
        }
        List<WebAuthnCredentialRecord> records = passkeys.ofMember(id).stream()
                .map(Passkey::toRecord)
                .toList();
        return Uni.createFrom().item(records);
    }

    @Override
    @Transactional
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        return passkeys.find(credentialId)
                .map(passkey -> Uni.createFrom().item(passkey.toRecord()))
                .orElseGet(() -> Uni.createFrom().failure(new IllegalArgumentException("Unknown passkey")));
    }

    @Override
    @Transactional
    public Uni<Void> update(String credentialId, long counter) {
        passkeys.find(credentialId).ifPresent(passkey -> passkey.counter = counter);
        return Uni.createFrom().voidItem();
    }

    private static UUID parse(String memberId) {
        if (memberId == null) {
            return null;
        }
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
