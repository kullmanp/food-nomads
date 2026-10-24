package ch.kup.foodnomads.api;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** The logged-in Member. */
@Path("/api/me")
public class MeResource {

    public record Me(String displayName) {
        static Me of(Member member) {
            return new Me(member.displayName);
        }
    }

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response me() {
        return membership.current(identity)
                .map(member -> Response.ok(Me.of(member)).build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
