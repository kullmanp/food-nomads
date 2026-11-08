package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.AlreadyMemberException;
import ch.kup.foodnomads.members.InviteRejectedException;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
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

        static native TemplateInstance alreadyMember();
    }

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response get(@PathParam("token") String token) {
        try {
            membership.checkCanJoin(identity, token);
        } catch (AlreadyMemberException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(Templates.alreadyMember())
                    .build();
        } catch (InviteRejectedException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(Templates.rejected())
                    .build();
        }
        return Response.ok(Templates.join(token)).build();
    }
}
