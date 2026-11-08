package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.Invites;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Creating an Invite and passing its link on. The form posts with htmx, which swaps in the
 * page with the new link; without JavaScript it is an ordinary form post that shows the same page.
 */
@Path("/invites")
@Produces(MediaType.TEXT_HTML)
public class InvitesPage {

    @CheckedTemplate
    static class Templates {
        /** @param link the link of the Invite just created, or null if there is none */
        static native TemplateInstance invites(String link);
    }

    @Inject
    Invites invites;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public TemplateInstance get() {
        return Templates.invites(null);
    }

    @POST
    public TemplateInstance create() {
        var creator = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        return Templates.invites(invites.joinUrl(invites.create(creator)));
    }
}
