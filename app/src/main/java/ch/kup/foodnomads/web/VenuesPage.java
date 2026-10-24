package ch.kup.foodnomads.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** The candidate Venues. Empty until Members can propose Venues. */
@Path("/venues")
public class VenuesPage {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance venues();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.venues();
    }
}
