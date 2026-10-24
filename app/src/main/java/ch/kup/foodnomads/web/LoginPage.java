package ch.kup.foodnomads.web;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Where pages that need a logged-in Member send everyone else. */
@Path("/login")
public class LoginPage {

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance login();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return Templates.login();
    }
}
