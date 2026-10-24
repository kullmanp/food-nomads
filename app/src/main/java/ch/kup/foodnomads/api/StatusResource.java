package ch.kup.foodnomads.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/status")
public class StatusResource {

    public record Status(String status) {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Status status() {
        return new Status("UP");
    }
}
