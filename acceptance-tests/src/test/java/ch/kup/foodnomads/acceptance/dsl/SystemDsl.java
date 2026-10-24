package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.SystemDriver;

/** DSL for the system as a whole, independent of any domain concept. */
public class SystemDsl {

    private final SystemDriver driver;

    SystemDsl(SystemDriver driver) {
        this.driver = driver;
    }

    public void assertAvailable() {
        driver.assertAvailable();
    }
}
