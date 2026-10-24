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
    public void propose(String name, String address) {
        throw new UnsupportedOperationException(
                "Not implemented yet: POST /api/venues answers 201 with the new Venue");
    }

    @Override
    public void assertProposalRejected(String name, String address) {
        throw new UnsupportedOperationException(
                "Not implemented yet: POST /api/venues refuses a Venue without a name");
    }

    @Override
    public void assertCandidate(String name, String address, String proposedBy) {
        throw new UnsupportedOperationException(
                "Not implemented yet: GET /api/venues lists the Venue with its address and proposing Member");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.get("api/venues");

        assertEquals(401, response.statusCode(), "GET /api/venues: " + response.body());
    }
}
