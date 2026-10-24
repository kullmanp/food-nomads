package ch.kup.foodnomads.members;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord.RequiredPersistedData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

/** A WebAuthn credential a Member logs in with. */
@Entity
public class Passkey extends PanacheEntityBase {

    @Id
    @Column(name = "credential_id")
    public String credentialId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id")
    public Member member;

    public UUID aaguid;

    @Column(name = "public_key", nullable = false)
    public byte[] publicKey;

    @Column(name = "public_key_algorithm", nullable = false)
    public long publicKeyAlgorithm;

    @Column(nullable = false)
    public long counter;

    static Passkey of(Member member, WebAuthnCredentialRecord record) {
        RequiredPersistedData data = record.getRequiredPersistedData();
        Passkey passkey = new Passkey();
        passkey.credentialId = data.credentialId();
        passkey.member = member;
        passkey.aaguid = data.aaguid();
        passkey.publicKey = data.publicKey();
        passkey.publicKeyAlgorithm = data.publicKeyAlgorithm();
        passkey.counter = data.counter();
        return passkey;
    }

    WebAuthnCredentialRecord toRecord() {
        return WebAuthnCredentialRecord.fromRequiredPersistedData(new RequiredPersistedData(
                member.id.toString(), credentialId, aaguid, publicKey, publicKeyAlgorithm, counter));
    }
}
