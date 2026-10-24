package ch.kup.foodnomads.acceptance.drivers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** HTTP with JSON against the app, keeping the session cookie like a browser would. */
final class ApiClient implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    private final URI baseUrl;

    ApiClient(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** The origin a browser on the app would report, e.g. {@code http://localhost:8080}. */
    String origin() {
        return baseUrl.getScheme() + "://" + baseUrl.getAuthority();
    }

    HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(baseUrl.resolve(path))
                .header("Accept", "application/json")
                .build());
    }

    HttpResponse<String> post(String path, JsonNode body) {
        return send(HttpRequest.newBuilder(baseUrl.resolve(path))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build());
    }

    static JsonNode readJson(HttpResponse<String> response) {
        try {
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new UncheckedIOException("Not JSON: " + response.body(), e);
        }
    }

    static ObjectNode newObject() {
        return JSON.createObjectNode();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        http.close();
    }
}
