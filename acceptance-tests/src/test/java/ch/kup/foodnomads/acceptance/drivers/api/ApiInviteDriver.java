package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.stream.StreamSupport;

class ApiInviteDriver implements InviteDriver {

    private final ApiClient api;

    ApiInviteDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public URI create() {
        HttpResponse<String> response = api.post("api/invites", ApiClient.newObject());

        assertEquals(201, response.statusCode(), "POST /api/invites: " + response.body());
        return URI.create(ApiClient.readJson(response).path("url").asText());
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        HttpResponse<String> response = api.post("api/invites", ApiClient.newObject());

        assertEquals(401, response.statusCode(), "POST /api/invites: " + response.body());
    }

    @Override
    public void assertOpen(URI link, String createdBy) {
        JsonNode invite = openInvite(link).orElseThrow(() -> new AssertionError(link + " is not listed as open"));

        assertEquals(createdBy, invite.path("createdBy").asText(), "createdBy in " + invite);
        assertTrue(invite.hasNonNull("expiresAt"), "expiresAt in " + invite);
    }

    @Override
    public void assertNotOpen(URI link) {
        assertEquals(Optional.empty(), openInvite(link), "Open Invite with link " + link);
    }

    @Override
    public void revoke(URI link) {
        JsonNode invite = openInvite(link).orElseThrow(() -> new AssertionError(link + " is not listed as open"));

        HttpResponse<String> response =
                api.delete("api/invites/" + invite.path("id").asText());

        assertEquals(204, response.statusCode(), "DELETE /api/invites/{id}: " + response.body());
    }

    /** The app builds links on its own base URL, so only the paths can be compared. */
    private Optional<JsonNode> openInvite(URI link) {
        HttpResponse<String> response = api.get("api/invites");
        assertEquals(200, response.statusCode(), "GET /api/invites: " + response.body());
        return StreamSupport.stream(ApiClient.readJson(response).spliterator(), false)
                .filter(invite ->
                        URI.create(invite.path("url").asText()).getPath().equals(link.getPath()))
                .findFirst();
    }
}
