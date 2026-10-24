package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;

class ApiVenueDriver implements VenueDriver {

    private final ApiClient api;

    ApiVenueDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException(
                "Not implemented yet: GET /api/venues answers 401 without a logged-in Member");
    }
}
