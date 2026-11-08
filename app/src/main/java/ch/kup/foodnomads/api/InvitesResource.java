package ch.kup.foodnomads.api;

import ch.kup.foodnomads.members.Invite;
import ch.kup.foodnomads.members.Invites;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.UUID;

/** Creating Invites. */
@Path("/api/invites")
@Produces(MediaType.APPLICATION_JSON)
public class InvitesResource {

    public record InviteJson(UUID id, String url, String createdBy, Instant expiresAt) {}

    @Inject
    Invites invites;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @POST
    public Response create() {
        var creator = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        Invite invite = invites.create(creator);
        var json = new InviteJson(invite.id, invites.joinUrl(invite), invite.createdBy.displayName, invite.expiresAt());
        return Response.status(Response.Status.CREATED).entity(json).build();
    }
}
