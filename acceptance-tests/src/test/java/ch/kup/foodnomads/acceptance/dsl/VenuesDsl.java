package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;

/**
 * DSL for Venues. Venue names are aliased for functional isolation; the proposing Member's name
 * isn't (see {@link MembersDsl}).
 */
public class VenuesDsl {

    private final VenueDriver driver;
    private final Aliases aliases;

    /** @param aliases the Venue aliases, shared with {@link LunchesDsl} */
    VenuesDsl(VenueDriver driver, Aliases aliases) {
        this.driver = driver;
        this.aliases = aliases;
    }

    /**
     * Proposes a Venue as the logged-in Member. With {@code expect: rejected}, fails unless the
     * proposal is refused.
     */
    public void propose(String... args) {
        Params params = Params.parse(
                args, Params.required("name"), Params.optional("address", ""), Params.optional("expect", "success"));
        String name = aliases.realName(params.value("name"));
        String address = params.value("address");
        switch (params.value("expect")) {
            case "success" -> driver.propose(name, address);
            case "rejected" -> driver.assertProposalRejected(name, address);
            default ->
                throw new IllegalArgumentException(
                        "Unknown expect '" + params.value("expect") + "', expected success or rejected");
        }
    }

    /** Fails unless the candidate list shows this Venue with this address and proposing Member. */
    public void assertCandidate(String... args) {
        Params params = Params.parse(
                args, Params.required("name"), Params.optional("address", ""), Params.required("proposedBy"));
        driver.assertCandidate(
                aliases.realName(params.value("name")), params.value("address"), params.value("proposedBy"));
    }

    /** Fails unless the Venue list marks this Venue as visited. */
    public void assertVisited(String... args) {
        Params params = Params.parse(args, Params.required("name"));
        driver.assertVisited(aliases.realName(params.value("name")));
    }

    /** Fails unless the Venue list shows this Venue as not visited. */
    public void assertNotVisited(String... args) {
        Params params = Params.parse(args, Params.required("name"));
        driver.assertNotVisited(aliases.realName(params.value("name")));
    }

    /** Fails unless the Venue list is refused to someone who isn't logged in. */
    public void assertNotAccessibleWithoutLogin() {
        driver.assertNotAccessibleWithoutLogin();
    }
}
