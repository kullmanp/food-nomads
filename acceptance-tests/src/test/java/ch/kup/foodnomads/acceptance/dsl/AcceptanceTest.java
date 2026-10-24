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

    @BeforeEach
    void setUpDsl() {
        Founder.ensureJoined();
        driver = ProtocolDriver.selected();
        system = new SystemDsl(driver.system());
        members = new MembersDsl(driver.members());
    }

    @AfterEach
    void closeDriver() {
        if (driver != null) {
            driver.close();
        }
    }
}
