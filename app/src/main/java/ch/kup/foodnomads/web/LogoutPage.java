package ch.kup.foodnomads.web;

import io.quarkus.security.webauthn.WebAuthnSecurity;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/logout")
public class LogoutPage {

    @Inject
    WebAuthnSecurity webAuthnSecurity;

    @POST
    public Response post(RoutingContext ctx) {
        webAuthnSecurity.logout(ctx);
        return Response.seeOther(URI.create("/")).build();
    }
}
