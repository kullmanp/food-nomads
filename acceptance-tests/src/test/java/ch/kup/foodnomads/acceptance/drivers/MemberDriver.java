package ch.kup.foodnomads.acceptance.drivers;

import java.net.URI;

public interface MemberDriver {

    /**
     * Joins through the given Invite link as a new Member with this display name, registering a
     * passkey and keeping it in {@link Passkeys} under the display name.
     */
    void join(URI joinUrl, String displayName);

    /** Fails unless this driver's session belongs to the Member with this display name. */
    void assertLoggedIn(String displayName);

    /** Fails unless registering a passkey through the given join URL is refused. */
    void assertBootstrapInviteRejected(URI joinUrl);
}
