package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.SystemDriver;

/** Talks to the system through the JSON API under {@code /api}. */
public class ApiProtocolDriver implements ProtocolDriver {

    @Override
    public SystemDriver system() {
        return new ApiSystemDriver();
    }
}
