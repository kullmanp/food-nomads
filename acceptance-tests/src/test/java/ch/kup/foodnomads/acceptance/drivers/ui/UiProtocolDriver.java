package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.SystemDriver;

/** Talks to the system through the browser, like a Member on their phone. */
public class UiProtocolDriver implements ProtocolDriver {

    @Override
    public SystemDriver system() {
        return new UiSystemDriver();
    }
}
