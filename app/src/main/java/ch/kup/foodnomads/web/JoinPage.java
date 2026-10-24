package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.Membership;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Where an Invite link leads: pick a display name and create a passkey. */
@Path("/join/{token}")
public class JoinPage {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance join(String token);

        static native TemplateInstance rejected();
    }

    @Inject
    Membership membership;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get(@PathParam("token") String token) {
        if (!membership.isInviteValid(token)) {
            return Response.status(Response.Status.FORBIDDEN).entity(Templates.rejected()).build();
        }
        return Response.ok(Templates.join(token)).build();
    }
}
