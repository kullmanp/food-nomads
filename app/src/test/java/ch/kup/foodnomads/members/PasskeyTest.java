package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.credentialId;
import static ch.kup.foodnomads.members.TestData.member;
import static ch.kup.foodnomads.members.TestData.registration;
import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import org.junit.jupiter.api.Test;

class PasskeyTest {

    @Test
    void roundTripsARegistrationUnderTheMembersId() {
        Member anna = member("Anna");
        WebAuthnCredentialRecord registration = registration(credentialId("anna-phone"));
        RequiredPersistedData registered = registration.getRequiredPersistedData();

        RequiredPersistedData stored = Passkey.of(anna, registration).toRecord().getRequiredPersistedData();

        assertThat(stored.username()).isEqualTo(anna.id.toString());
        assertThat(stored.credentialId()).isEqualTo(credentialId("anna-phone"));
        assertThat(stored.aaguid()).isEqualTo(registered.aaguid());
        assertThat(stored.publicKey()).isEqualTo(registered.publicKey());
        assertThat(stored.publicKeyAlgorithm()).isEqualTo(TestData.ES256);
        assertThat(stored.counter()).isEqualTo(5L);
    }
}
