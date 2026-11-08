package ch.kup.foodnomads.api;

import ch.kup.foodnomads.members.Invite;
import ch.kup.foodnomads.members.Invites;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Creating Invites, listing the Open Invites and revoking them. */
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

    @GET
    public List<InviteJson> open() {
        return invites.open().stream().map(this::toJson).toList();
    }

    @POST
    public Response create() {
        var creator = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        return Response.status(Response.Status.CREATED)
                .entity(toJson(invites.create(creator)))
                .build();
    }

    @DELETE
    @Path("{id}")
    public Response revoke(@PathParam("id") UUID id) {
        membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        invites.revoke(id);
        return Response.noContent().build();
    }

    private InviteJson toJson(Invite invite) {
        return new InviteJson(invite.id, invites.joinUrl(invite), invite.createdBy.displayName, invite.expiresAt());
    }
}
