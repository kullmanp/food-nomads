package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all acceptance tests. Exposes the DSL entry points, backed by the
 * protocol driver chosen with {@code -Ddriver=api|ui}.
 */
public abstract class AcceptanceTest {

    private ProtocolDriver driver;

    protected SystemDsl system;
    protected MembersDsl members;
    protected VenuesDsl venues;
    protected InvitesDsl invites;

    @BeforeEach
    void setUpDsl() {
        Founder.ensureJoined();
        driver = ProtocolDriver.selected();
        system = new SystemDsl(driver.system());
        InviteLinks inviteLinks = new InviteLinks();
        members = new MembersDsl(driver.members(), inviteLinks);
        venues = new VenuesDsl(driver.venues());
        invites = new InvitesDsl(driver.invites(), inviteLinks, members);
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
