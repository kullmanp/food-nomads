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
}
