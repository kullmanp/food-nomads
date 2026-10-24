package ch.kup.foodnomads.api;

import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/logout")
public class LogoutResource {

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @POST
    public Response logOut(RoutingContext ctx) {
        webAuthnSecurity.logout(ctx);
        return Response.noContent().build();
    }
}
