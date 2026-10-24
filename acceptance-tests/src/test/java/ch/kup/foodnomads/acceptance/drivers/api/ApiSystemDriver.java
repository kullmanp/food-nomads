package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpResponse;

class ApiSystemDriver implements SystemDriver {

    private final ApiClient api;

    ApiSystemDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public void assertAvailable() {
        HttpResponse<String> response = api.get("api/status");

        assertEquals(200, response.statusCode(), "GET /api/status");
        JsonNode body = ApiClient.readJson(response);
        assertEquals("UP", body.path("status").asText(), "status in " + response.body());
    }
}
