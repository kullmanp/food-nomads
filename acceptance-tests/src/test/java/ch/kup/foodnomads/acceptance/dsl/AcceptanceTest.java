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
    protected LunchesDsl lunches;

    @BeforeEach
    void setUpDsl() {
        Founder.ensureJoined();
        driver = ProtocolDriver.selected();
        system = new SystemDsl(driver.system());
        InviteLinks inviteLinks = new InviteLinks();
        members = new MembersDsl(driver.members(), inviteLinks);
        Aliases venueAliases = new Aliases();
        venues = new VenuesDsl(driver.venues(), venueAliases);
        invites = new InvitesDsl(driver.invites(), inviteLinks, members);
        lunches = new LunchesDsl(driver.lunches(), venueAliases, members);
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
