package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import java.net.URI;

class ApiInviteDriver implements InviteDriver {

    private final ApiClient api;

    ApiInviteDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public URI create() {
        throw new UnsupportedOperationException("Not implemented yet: POST /api/invites answers 201 with the link");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException("Not implemented yet: POST /api/invites answers 401 without login");
    }
}
