package ch.kup.foodnomads.members;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Joining the group and finding out who is logged in. */
@ApplicationScoped
public class Membership {

    @Inject
    BootstrapInvite bootstrapInvite;

    public boolean isInviteValid(String token) {
        return bootstrapInvite.isValid(token);
    }

    /** Fails with {@link InviteRejectedException} unless the token belongs to a valid Invite. */
    public void checkInvite(String token) {
        if (!isInviteValid(token)) {
            throw new InviteRejectedException();
        }
    }

    /**
     * Creates a Member with the passkey they just registered, redeeming the Invite.
     *
     * @param passkey a verified registration, whose username is ignored
     */
    @Transactional
    public Member join(String token, String displayName, WebAuthnCredentialRecord passkey) {
        String name = displayName == null ? "" : displayName.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A display name is required");
        }
        bootstrapInvite.consume(token);

        Member member = new Member();
        member.id = UUID.randomUUID();
        member.displayName = name;
        member.joinedAt = Instant.now();
        member.persist();
        Passkey.of(member, passkey).persist();
        return member;
    }

    public Optional<Member> current(SecurityIdentity identity) {
        if (identity.isAnonymous()) {
            return Optional.empty();
        }
        return find(identity.getPrincipal().getName());
    }

    /** The Member with this id, which is also their WebAuthn username and principal name. */
    @Transactional
    public Optional<Member> find(String memberId) {
        if (memberId == null) {
            return Optional.empty();
        }
        try {
            return Member.findByIdOptional(UUID.fromString(memberId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
