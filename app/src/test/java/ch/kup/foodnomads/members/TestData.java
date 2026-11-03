package ch.kup.foodnomads.members;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

final class TestData {

    /** COSE algorithm ES256. */
    static final long ES256 = -7L;

    private static final byte[] PUBLIC_KEY = ecPublicKey();

    private TestData() {}

    static Member member(String displayName) {
        Member member = new Member();
        member.id = UUID.randomUUID();
        member.displayName = displayName;
        member.joinedAt = Instant.parse("2026-01-01T12:00:00Z");
        return member;
    }

    /** A credential id, base64url-encoded as WebAuthn expects, made from a readable label. */
    static String credentialId(String label) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(label.getBytes(StandardCharsets.UTF_8));
    }

    /** A registration as WebAuthn hands it over; its username is whatever the browser sent. */
    static WebAuthnCredentialRecord registration(String credentialId) {
        return WebAuthnCredentialRecord.fromRequiredPersistedData(new RequiredPersistedData(
                "ignored-username", credentialId, UUID.randomUUID(), PUBLIC_KEY, ES256, 5L));
    }

    static Passkey passkey(Member member, String credentialId) {
        return Passkey.of(member, registration(credentialId));
    }

    private static byte[] ecPublicKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            return generator.generateKeyPair().getPublic().getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
