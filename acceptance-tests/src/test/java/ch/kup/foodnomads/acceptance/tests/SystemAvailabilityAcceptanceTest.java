package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class SystemAvailabilityAcceptanceTest extends AcceptanceTest {

    @Test
    void systemIsAvailable() {
        system.assertAvailable();
    }
}
