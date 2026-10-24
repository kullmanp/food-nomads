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

    @Override
    @Transactional
    public Uni<List<WebAuthnCredentialRecord>> findByUsername(String memberId) {
        List<WebAuthnCredentialRecord> records = Passkey.<Passkey>list("member.id", parse(memberId)).stream()
                .map(Passkey::toRecord)
                .toList();
        return Uni.createFrom().item(records);
    }

    @Override
    @Transactional
    public Uni<WebAuthnCredentialRecord> findByCredentialId(String credentialId) {
        Passkey passkey = Passkey.findById(credentialId);
        if (passkey == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Unknown passkey"));
        }
        return Uni.createFrom().item(passkey.toRecord());
    }

    @Override
    @Transactional
    public Uni<Void> update(String credentialId, long counter) {
        Passkey passkey = Passkey.findById(credentialId);
        if (passkey != null) {
            passkey.counter = counter;
        }
        return Uni.createFrom().voidItem();
    }

    private static UUID parse(String memberId) {
        try {
            return UUID.fromString(memberId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
