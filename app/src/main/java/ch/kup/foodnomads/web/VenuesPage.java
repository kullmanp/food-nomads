package ch.kup.foodnomads.web;

import ch.kup.foodnomads.members.Membership;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.Venues;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * The candidate Venues, newest first, with a form to propose one. The form posts with htmx,
 * which swaps in the re-rendered list; without JavaScript it is an ordinary form post.
 */
@Path("/venues")
@Produces(MediaType.TEXT_HTML)
public class VenuesPage {

    /** The group lunches in Basel, so that's the time zone dates are shown in. */
    private static final ZoneId ZONE = ZoneId.of("Europe/Zurich");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    public record Candidate(String name, String address, String proposedBy, String proposedAt, String proposedOn) {
        static Candidate of(Venue venue) {
            var proposedAt = venue.proposedAt.atZone(ZONE);
            return new Candidate(venue.name, venue.address, venue.proposedBy.displayName,
                    proposedAt.toOffsetDateTime().toString(), DATE.format(proposedAt));
        }
    }

    /** What the propose form shows after a rejected proposal. */
    public record Draft(String name, String address, boolean rejected) {
        static final Draft EMPTY = new Draft("", "", false);
    }

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance venues(List<Candidate> candidates, Draft draft);
    }

    @Inject
    Venues venues;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public TemplateInstance get() {
        return page(Draft.EMPTY);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response propose(@FormParam("name") String name, @FormParam("address") String address,
            @HeaderParam("HX-Request") String htmx) {
        var proposer = membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
        try {
            venues.propose(proposer, name, address);
        } catch (IllegalArgumentException e) {
            var draft = new Draft(name == null ? "" : name, address == null ? "" : address, true);
            return Response.status(422).entity(page(draft)).build();
        }
        if (htmx == null) {
            return Response.seeOther(URI.create("/venues")).build();
        }
        return Response.ok(page(Draft.EMPTY)).build();
    }

    private TemplateInstance page(Draft draft) {
        return Templates.venues(venues.candidates().stream().map(Candidate::of).toList(), draft);
    }
}
