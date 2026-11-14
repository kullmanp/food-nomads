package ch.kup.foodnomads.web;

import ch.kup.foodnomads.lunches.Lunches;
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
        /**
         * @param currentMember the logged-in Member's display name, or null for a visitor
         * @param plannedLunch the Planned Lunch, or null if there is none or nobody is logged in
         */
        static native TemplateInstance home(String currentMember, LunchesPage.LunchRow plannedLunch);
    }

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @Inject
    Lunches lunches;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        var member = membership.current(identity);
        if (member.isEmpty()) {
            return Templates.home(null, null);
        }
        return Templates.home(
                member.get().displayName(),
                lunches.planned().map(LunchesPage.LunchRow::of).orElse(null));
    }
}
