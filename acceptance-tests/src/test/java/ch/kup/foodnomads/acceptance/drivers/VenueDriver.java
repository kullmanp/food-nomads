package ch.kup.foodnomads.acceptance.drivers;

public interface VenueDriver {

    /**
     * Proposes a Venue as the logged-in Member and fails unless it was accepted. An empty address
     * means none.
     */
    void propose(String name, String address);

    /** Fails unless proposing this Venue as the logged-in Member is refused. */
    void assertProposalRejected(String name, String address);

    /**
     * Fails unless the candidate list shows a Venue with this name, address (empty means none) and
     * proposing Member's display name.
     */
    void assertCandidate(String name, String address, String proposedBy);

    /**
     * Fails unless this driver, with no Member logged in, is kept away from the Venue list: the
     * UI sends it to login, the API answers 401.
     */
    void assertNotAccessibleWithoutLogin();
}
