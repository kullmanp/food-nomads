package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import java.net.http.HttpResponse;

class ApiVenueDriver implements VenueDriver {

    private final ApiClient api;

    ApiVenueDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.get("api/venues");

        assertEquals(401, response.statusCode(), "GET /api/venues: " + response.body());
    }
}
