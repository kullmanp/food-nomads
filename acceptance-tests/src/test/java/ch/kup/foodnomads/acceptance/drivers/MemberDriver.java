package ch.kup.foodnomads.acceptance.drivers;

import java.net.URI;

public interface MemberDriver {

    /**
     * Joins through the given Invite link as a new Member with this display name, registering a
     * passkey and keeping it in {@link Passkeys} under the display name.
     */
    void join(URI joinUrl, String displayName);

    /**
     * Logs in with the passkey this Member registered when joining, imported from
     * {@link Passkeys} into this driver's fresh authenticator.
     */
    void logIn(String displayName);

    void logOut();

    /** Fails unless this driver's session belongs to the Member with this display name. */
    void assertLoggedIn(String displayName);

    /** Fails unless no Member is logged in on this driver's session. */
    void assertLoggedOut();

    /** Fails unless registering a passkey through the given join URL is refused. */
    void assertBootstrapInviteRejected(URI joinUrl);
}
