package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpResponse;
import java.util.stream.StreamSupport;

class ApiVenueDriver implements VenueDriver {

    private final ApiClient api;

    ApiVenueDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public void propose(String name, String address) {
        HttpResponse<String> response = api.post("api/venues", proposeRequest(name, address));

        assertEquals(201, response.statusCode(), "POST /api/venues: " + response.body());
    }

    @Override
    public void assertProposalRejected(String name, String address) {
        HttpResponse<String> response = api.post("api/venues", proposeRequest(name, address));

        assertEquals(400, response.statusCode(), "POST /api/venues: " + response.body());
    }

    @Override
    public void assertCandidate(String name, String address, String proposedBy) {
        JsonNode venue = listed(name);
        assertEquals(address, venue.path("address").asText(""), "address in " + venue);
        assertEquals(proposedBy, venue.path("proposedBy").asText(), "proposedBy in " + venue);
    }

    @Override
    public void assertVisited(String name) {
        JsonNode venue = listed(name);
        assertTrue(venue.path("visited").isBoolean(), "visited in " + venue);
        assertTrue(venue.path("visited").asBoolean(), "visited in " + venue);
    }

    @Override
    public void assertNotVisited(String name) {
        JsonNode venue = listed(name);
        assertTrue(venue.path("visited").isBoolean(), "visited in " + venue);
        assertFalse(venue.path("visited").asBoolean(), "visited in " + venue);
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.get("api/venues");

        assertEquals(401, response.statusCode(), "GET /api/venues: " + response.body());
    }

    /** The Venue with this name as {@code GET /api/venues} lists it. */
    private JsonNode listed(String name) {
        HttpResponse<String> response = api.get("api/venues");
        assertEquals(200, response.statusCode(), "GET /api/venues: " + response.body());
        return StreamSupport.stream(ApiClient.readJson(response).spliterator(), false)
                .filter(v -> v.path("name").asText().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Venue '" + name + "' in " + response.body()));
    }

    private static ObjectNode proposeRequest(String name, String address) {
        return ApiClient.newObject().put("name", name).put("address", address);
    }
}
