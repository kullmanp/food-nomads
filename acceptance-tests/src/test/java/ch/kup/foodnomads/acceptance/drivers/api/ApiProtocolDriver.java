package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import java.net.URI;
import java.net.http.HttpClient;

/** Talks to the system through the JSON API under {@code /api}. */
public class ApiProtocolDriver implements ProtocolDriver {

    private final HttpClient http = HttpClient.newHttpClient();
    private final URI baseUrl;

    public ApiProtocolDriver(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public SystemDriver system() {
        return new ApiSystemDriver(http, baseUrl);
    }

    @Override
    public void close() {
        http.close();
    }
}
