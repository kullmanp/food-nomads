package ch.kup.foodnomads.api;

import ch.kup.foodnomads.members.Membership;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.Venues;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The candidate Venues, newest first, and proposing a new one. */
@Path("/api/venues")
@Produces(MediaType.APPLICATION_JSON)
public class VenuesResource {

    public record VenueJson(UUID id, String name, String address, String proposedBy, Instant proposedAt) {
        static VenueJson of(Venue venue) {
            return new VenueJson(venue.id, venue.name, venue.address, venue.proposedBy.displayName,
                    venue.proposedAt);
        }
    }

    public record ProposeRequest(String name, String address) {
    }

    @Inject
    Venues venues;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public List<VenueJson> candidates() {
        return venues.candidates().stream().map(VenueJson::of).toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response propose(ProposeRequest request) {
        var proposer = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        if (request == null) {
            return nameRequired();
        }
        try {
            Venue venue = venues.propose(proposer, request.name(), request.address());
            return Response.status(Response.Status.CREATED).entity(VenueJson.of(venue)).build();
        } catch (IllegalArgumentException e) {
            return nameRequired();
        }
    }

    private static Response nameRequired() {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", "name-required")).build();
    }
}
