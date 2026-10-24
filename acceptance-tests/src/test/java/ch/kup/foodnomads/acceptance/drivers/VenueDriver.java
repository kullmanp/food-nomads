package ch.kup.foodnomads.acceptance.drivers;

public interface VenueDriver {

    /**
     * Fails unless this driver, with no Member logged in, is kept away from the Venue list: the
     * UI sends it to login, the API answers 401.
     */
    void assertNotAccessibleWithoutLogin();
}
