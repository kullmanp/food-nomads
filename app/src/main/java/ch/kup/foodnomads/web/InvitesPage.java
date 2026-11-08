package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.Invite;
import ch.kup.foodnomads.members.Invites;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Creating an Invite, passing its link on, and the Open Invites, newest first. The forms post
 * with htmx, which swaps in the re-rendered page; without JavaScript they are ordinary form
 * posts that come back to the list, where the new link is too.
 */
@Path("/invites")
@Produces(MediaType.TEXT_HTML)
public class InvitesPage {

    private static final URI LIST = URI.create("/invites");

    public record OpenInvite(UUID id, String link, String createdBy, String expiresAt, String expiresOn) {
        static OpenInvite of(Invite invite, String link) {
            var expiresAt = invite.expiresAt().atZone(VenuesPage.ZONE);
            return new OpenInvite(
                    invite.id,
                    link,
                    invite.createdBy.displayName,
                    expiresAt.toOffsetDateTime().toString(),
                    VenuesPage.DATE.format(expiresAt));
        }
    }

    @CheckedTemplate
    static class Templates {
        /** @param link the link of the Invite just created, or null if there is none */
        static native TemplateInstance invites(String link, List<OpenInvite> openInvites);
    }

    @Inject
    Invites invites;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public TemplateInstance get() {
        return page(null);
    }

    @POST
    public Response create(@HeaderParam("HX-Request") String htmx) {
        var creator = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        Invite invite = invites.create(creator);
        if (htmx == null) {
            return Response.seeOther(LIST).build();
        }
        return Response.ok(page(invites.joinUrl(invite))).build();
    }

    @POST
    @Path("{id}/revoke")
    public Response revoke(@PathParam("id") UUID id, @HeaderParam("HX-Request") String htmx) {
        membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        invites.revoke(id);
        if (htmx == null) {
            return Response.seeOther(LIST).build();
        }
        return Response.ok(page(null)).build();
    }

    private TemplateInstance page(String link) {
        var openInvites = invites.open().stream()
                .map(invite -> OpenInvite.of(invite, invites.joinUrl(invite)))
                .toList();
        return Templates.invites(link, openInvites);
    }
}
