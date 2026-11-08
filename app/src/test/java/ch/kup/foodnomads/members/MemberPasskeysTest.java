package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.credentialId;
import static ch.kup.foodnomads.members.TestData.member;
import static ch.kup.foodnomads.members.TestData.passkey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemberPasskeysTest {

    private final InMemoryPasskeyRepository passkeys = new InMemoryPasskeyRepository();
    private final MemberPasskeys provider = new MemberPasskeys(passkeys);
    private final Member anna = member("Anna");
    private final Member ben = member("Ben");

    @Test
    void findsAMembersPasskeysByTheirId() {
        passkeys.add(passkey(anna, credentialId("anna-phone")));
        passkeys.add(passkey(anna, credentialId("anna-laptop")));
        passkeys.add(passkey(ben, credentialId("ben-phone")));

        List<WebAuthnCredentialRecord> records =
                provider.findByUsername(anna.id.toString()).await().indefinitely();

        assertThat(records)
                .extracting(WebAuthnCredentialRecord::getCredentialID)
                .containsExactlyInAnyOrder(credentialId("anna-phone"), credentialId("anna-laptop"));
        assertThat(records).extracting(WebAuthnCredentialRecord::getUsername).containsOnly(anna.id.toString());
    }

    @Test
    void findsNoPasskeysForAMalformedOrMissingUsername() {
        passkeys.add(passkey(anna, credentialId("anna-phone")));

        assertThat(provider.findByUsername("not-a-uuid").await().indefinitely()).isEmpty();
        assertThat(provider.findByUsername(null).await().indefinitely()).isEmpty();
    }

    @Test
    void findsAPasskeyByCredentialId() {
        passkeys.add(passkey(anna, credentialId("anna-phone")));

        WebAuthnCredentialRecord record =
                provider.findByCredentialId(credentialId("anna-phone")).await().indefinitely();

        assertThat(record.getCredentialID()).isEqualTo(credentialId("anna-phone"));
        assertThat(record.getUsername()).isEqualTo(anna.id.toString());
        assertThat(record.getCounter()).isEqualTo(5L);
    }

    @Test
    void failsForAnUnknownCredentialId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> provider.findByCredentialId(credentialId("unknown"))
                        .await()
                        .indefinitely());
    }

    @Test
    void updatesTheSignatureCounter() {
        passkeys.add(passkey(anna, credentialId("anna-phone")));

        provider.update(credentialId("anna-phone"), 42L).await().indefinitely();

        assertThat(passkeys.find(credentialId("anna-phone")).orElseThrow().counter)
                .isEqualTo(42L);
    }

    @Test
    void ignoresACounterUpdateForAnUnknownCredential() {
        provider.update(credentialId("unknown"), 42L).await().indefinitely();

        assertThat(passkeys.find(credentialId("unknown"))).isEmpty();
    }
}
