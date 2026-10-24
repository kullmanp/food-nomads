package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.SystemDriver;

class ApiSystemDriver implements SystemDriver {

    @Override
    public void assertAvailable() {
        throw new UnsupportedOperationException("Not implemented yet: GET /api/status");
    }
}
