package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public class HomePage {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance home(String currentMember);
    }

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.home(membership.current(identity).map(Member::displayName).orElse(null));
    }
}
