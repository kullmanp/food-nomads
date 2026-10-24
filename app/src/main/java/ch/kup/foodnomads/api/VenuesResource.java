package ch.kup.foodnomads.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/** The candidate Venues. Empty until Members can propose Venues. */
@Path("/api/venues")
public class VenuesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Object> venues() {
        return List.of();
    }
}
