package ch.kup.foodnomads.api;

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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

/**
 * Logging in with a passkey: the WebAuthn login ceremony. The options path works with
 * {@code loginClientSteps} from Quarkus's {@code webauthn.js}, so the login page uses these
 * endpoints too. No username is asked for: the authenticator offers the passkeys it holds.
 */
@Path("/api/login")
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {

    @Inject
    Membership membership;

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @GET
    @Path("options")
    public Response options(RoutingContext ctx) {
        var options = webAuthnSecurity.getLoginChallenge(null, ctx).await().indefinitely();
        return Response.ok(webAuthnSecurity.toJsonString(options)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response logIn(Map<String, Object> credential, RoutingContext ctx) {
        if (credential == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        WebAuthnCredentialRecord passkey;
        try {
            passkey = webAuthnSecurity.login(new JsonObject(credential), ctx).await().indefinitely();
        } catch (RuntimeException e) {
            return passkeyRejected();
        }

        Optional<Member> member = membership.find(passkey.getUsername());
        if (member.isEmpty()) {
            return passkeyRejected();
        }
        webAuthnSecurity.rememberUser(member.get().id.toString(), ctx);
        return Response.ok(MeResource.Me.of(member.get())).build();
    }

    private static Response passkeyRejected() {
        return Response.status(Response.Status.UNAUTHORIZED).entity(Map.of("error", "passkey-rejected")).build();
    }
}
