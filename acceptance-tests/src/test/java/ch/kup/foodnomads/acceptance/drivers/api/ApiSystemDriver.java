package ch.kup.foodnomads.acceptance.drivers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

class ApiSystemDriver implements SystemDriver {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final URI baseUrl;

    ApiSystemDriver(HttpClient http, URI baseUrl) {
        this.http = http;
        this.baseUrl = baseUrl;
    }

    @Override
    public void assertAvailable() {
        HttpResponse<String> response = get("api/status");

        assertEquals(200, response.statusCode(), "GET /api/status");
        JsonNode body = readJson(response.body());
        assertEquals("UP", body.path("status").asText(), "status in " + response.body());
    }

    private HttpResponse<String> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve(path))
                .header("Accept", "application/json")
                .build();
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static JsonNode readJson(String body) {
        try {
            return JSON.readTree(body);
        } catch (IOException e) {
            throw new UncheckedIOException("Not JSON: " + body, e);
        }
    }
}
