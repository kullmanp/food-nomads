package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;

/** DSL for Venues. */
public class VenuesDsl {

    private final VenueDriver driver;

    VenuesDsl(VenueDriver driver) {
        this.driver = driver;
    }

    /** Fails unless the Venue list is refused to someone who isn't logged in. */
    public void assertNotAccessibleWithoutLogin() {
        driver.assertNotAccessibleWithoutLogin();
    }
}
