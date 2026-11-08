package ch.kup.foodnomads.acceptance.drivers;

import java.net.URI;

public interface InviteDriver {

    /** Creates an Invite as the logged-in Member and returns the link the app handed out. */
    URI create();

    /**
     * Fails unless this driver, with no Member logged in, is kept away from creating Invites: the
     * UI sends it to login, the API answers 401.
     */
    void assertNotAccessibleWithoutLogin();

    /** Fails unless the Invite with this link is listed as open, created by this Member and with an expiry. */
    void assertOpen(URI link, String createdBy);

    /** Fails unless the list of Open Invites, as seen by the logged-in Member, lacks this link. */
    void assertNotOpen(URI link);

    /** Revokes the Open Invite with this link as the logged-in Member. */
    void revoke(URI link);
}
