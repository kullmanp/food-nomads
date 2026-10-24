package ch.kup.foodnomads.api;

import ch.kup.foodnomads.members.InviteRejectedException;
import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.security.webauthn.WebAuthnCredentialRecord;
import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Joining through an Invite: the WebAuthn registration ceremony, accepted only with a valid
 * Invite token. The options path works with {@code registerClientSteps} from Quarkus's
 * {@code webauthn.js}, so the join page uses these endpoints too.
 */
@Path("/api/join/{token}")
@Produces(MediaType.APPLICATION_JSON)
public class JoinResource {

    public record JoinRequest(String displayName, Map<String, Object> credential) {
    }

    @Inject
    Membership membership;

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @GET
    @Path("options")
    public Response options(@PathParam("token") String token, @QueryParam("displayName") String displayName,
            RoutingContext ctx) {
        try {
            membership.checkInvite(token);
        } catch (InviteRejectedException e) {
            return inviteRejected();
        }
        String name = displayName == null || displayName.isBlank() ? "Member" : displayName.strip();
        // The authenticator shows the WebAuthn username, so we pass the display name. The
        // username stored with the passkey is the Member id, set when the Member is created.
        var options = webAuthnSecurity.getRegisterChallenge(name, name, ctx).await().indefinitely();
        return Response.ok(webAuthnSecurity.toJsonString(options)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response join(@PathParam("token") String token, JoinRequest request, RoutingContext ctx) {
        try {
            membership.checkInvite(token);
        } catch (InviteRejectedException e) {
            return inviteRejected();
        }
        if (request == null || request.credential() == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        WebAuthnCredentialRecord passkey;
        try {
            passkey = webAuthnSecurity.register("pending", new JsonObject(request.credential()), ctx)
                    .await().indefinitely();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "passkey-rejected")).build();
        }

        Member member;
        try {
            member = membership.join(token, request.displayName(), passkey);
        } catch (InviteRejectedException e) {
            return inviteRejected();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "display-name-required")).build();
        }
        webAuthnSecurity.rememberUser(member.id.toString(), ctx);
        return Response.status(Response.Status.CREATED).entity(MeResource.Me.of(member)).build();
    }

    private static Response inviteRejected() {
        return Response.status(Response.Status.FORBIDDEN).entity(Map.of("error", "invite-rejected")).build();
    }
}
