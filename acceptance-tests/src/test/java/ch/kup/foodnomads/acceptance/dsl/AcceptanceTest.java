package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all acceptance tests. Exposes the DSL entry points, backed by the
 * protocol driver chosen with {@code -Ddriver=api|ui}.
 */
public abstract class AcceptanceTest {

    protected SystemDsl system;

    @BeforeEach
    void setUpDsl() {
        ProtocolDriver driver = ProtocolDriver.selected();
        system = new SystemDsl(driver.system());
    }
}
