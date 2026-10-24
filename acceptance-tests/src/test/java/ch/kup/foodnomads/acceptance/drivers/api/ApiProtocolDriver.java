package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import java.net.URI;

/** Talks to the system through the JSON API under {@code /api}. */
public class ApiProtocolDriver implements ProtocolDriver {

    private final ApiClient api;

    public ApiProtocolDriver(URI baseUrl) {
        this.api = new ApiClient(baseUrl);
    }

    @Override
    public SystemDriver system() {
        return new ApiSystemDriver(api);
    }

    @Override
    public MemberDriver members() {
        return new ApiMemberDriver(api);
    }

    @Override
    public VenueDriver venues() {
        return new ApiVenueDriver(api);
    }

    @Override
    public void close() {
        api.close();
    }
}
