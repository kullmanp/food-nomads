package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.Passkeys;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

class ApiMemberDriver implements MemberDriver {

    private final ApiClient api;

    ApiMemberDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public void join(URI joinUrl, String displayName) {
        String joinPath = joinApiPath(joinUrl);

        HttpResponse<String> options = api.get(joinPath + "/options?displayName=" + encode(displayName));
        assertEquals(200, options.statusCode(), "GET /" + joinPath + "/options: " + options.body());

        var registration = SoftwareAuthenticator.register(ApiClient.readJson(options), api.origin());
        HttpResponse<String> joined = api.post(joinPath, joinRequest(displayName, registration.credential()));
        assertEquals(201, joined.statusCode(), "POST /" + joinPath + ": " + joined.body());

        Passkeys.store(displayName, registration.passkey());
    }

    @Override
    public void assertLoggedIn(String displayName) {
        HttpResponse<String> response = api.get("api/me");

        assertEquals(200, response.statusCode(), "GET /api/me");
        assertEquals(displayName, ApiClient.readJson(response).path("displayName").asText(),
                "displayName in " + response.body());
    }

    @Override
    public void logIn(String displayName) {
        throw new UnsupportedOperationException(
                "Not implemented yet: log in through the WebAuthn login ceremony with the Member's imported passkey");
    }

    @Override
    public void logOut() {
        throw new UnsupportedOperationException(
                "Not implemented yet: log out through /api");
    }

    @Override
    public void assertLoggedOut() {
        throw new UnsupportedOperationException(
                "Not implemented yet: GET /api/me answers 401 once logged out");
    }

    @Override
    public void assertBootstrapInviteRejected(URI joinUrl) {
        String joinPath = joinApiPath(joinUrl);

        HttpResponse<String> options = api.get(joinPath + "/options?displayName=Intruder");
        assertEquals(403, options.statusCode(), "GET /" + joinPath + "/options: " + options.body());

        // Skipping the Invite's options step must not help: take a challenge from Quarkus's
        // own endpoint and present a valid passkey for it.
        HttpResponse<String> challenge = api.get(
                "q/webauthn/register-options-challenge?username=Intruder&displayName=Intruder");
        assertEquals(200, challenge.statusCode(), "GET /q/webauthn/register-options-challenge");
        var registration = SoftwareAuthenticator.register(ApiClient.readJson(challenge), api.origin());

        HttpResponse<String> joined = api.post(joinPath, joinRequest("Intruder", registration.credential()));
        assertEquals(403, joined.statusCode(), "POST /" + joinPath + ": " + joined.body());
    }

    /** Invite links look like {@code /join/<token>}; the API equivalent is {@code /api/join/<token>}. */
    private static String joinApiPath(URI joinUrl) {
        String path = joinUrl.getPath();
        String token = path.substring(path.lastIndexOf('/') + 1);
        return "api/join/" + token;
    }

    private static JsonNode joinRequest(String displayName, ObjectNode credential) {
        ObjectNode body = ApiClient.newObject();
        body.put("displayName", displayName);
        body.set("credential", credential);
        return body;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
