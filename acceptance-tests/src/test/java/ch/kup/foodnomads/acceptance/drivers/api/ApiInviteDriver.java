package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import java.net.URI;
import java.net.http.HttpResponse;

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
        throw new UnsupportedOperationException("Not implemented yet: GET /api/invites lists the Open Invites");
    }

    @Override
    public void assertNotOpen(URI link) {
        throw new UnsupportedOperationException("Not implemented yet: GET /api/invites lists the Open Invites");
    }

    @Override
    public void revoke(URI link) {
        throw new UnsupportedOperationException("Not implemented yet: DELETE /api/invites/{id} revokes an Invite");
    }
}
