package ch.kup.foodnomads.api;

import ch.kup.foodnomads.lunches.CommentTextRequiredException;
import ch.kup.foodnomads.lunches.DateInPastException;
import ch.kup.foodnomads.lunches.Lunch;
import ch.kup.foodnomads.lunches.LunchAlreadyPlannedException;
import ch.kup.foodnomads.lunches.LunchComment;
import ch.kup.foodnomads.lunches.LunchNotFoundException;
import ch.kup.foodnomads.lunches.LunchNotPlannedException;
import ch.kup.foodnomads.lunches.Lunches;
import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Planning and cancelling Lunches, and their Lunch Comments. */
@Path("/api/lunches")
@Produces(MediaType.APPLICATION_JSON)
public class LunchesResource {

    /** A Lunch's Venue. Lunches have none until a later ticket lets Members pick one. */
    public record VenueJson(UUID id, String name) {}

    public record LunchSummaryJson(UUID id, LocalDate date, String state, VenueJson venue) {
        static LunchSummaryJson of(Lunch lunch) {
            return new LunchSummaryJson(lunch.id, lunch.date, stateOf(lunch), null);
        }
    }

    public record LunchDetailJson(UUID id, LocalDate date, String state, VenueJson venue, List<CommentJson> comments) {
        static LunchDetailJson of(Lunch lunch) {
            return new LunchDetailJson(
                    lunch.id,
                    lunch.date,
                    stateOf(lunch),
                    null,
                    lunch.chat().stream().map(CommentJson::of).toList());
        }
    }

    public record CommentJson(UUID id, String author, String text, Instant writtenAt) {
        static CommentJson of(LunchComment comment) {
            return new CommentJson(comment.id, comment.author.displayName, comment.text, comment.writtenAt);
        }
    }

    public record PlanRequest(LocalDate date) {}

    public record CommentRequest(String text) {}

    @Inject
    Lunches lunches;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public List<LunchSummaryJson> all() {
        return lunches.all().stream().map(LunchSummaryJson::of).toList();
    }

    @GET
    @Path("planned")
    public Response planned() {
        return lunches.planned()
                .map(lunch -> Response.ok(LunchSummaryJson.of(lunch)).build())
                .orElseGet(() -> Response.noContent().build());
    }

    @GET
    @Path("{id}")
    public LunchDetailJson lunch(@PathParam("id") UUID id) {
        try {
            return LunchDetailJson.of(lunches.lunch(id));
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response plan(PlanRequest request) {
        currentMember();
        if (request == null || request.date() == null) {
            return error(Response.Status.BAD_REQUEST, "date-required");
        }
        try {
            Lunch lunch = lunches.plan(request.date());
            return Response.status(Response.Status.CREATED)
                    .entity(LunchSummaryJson.of(lunch))
                    .build();
        } catch (DateInPastException e) {
            return error(Response.Status.BAD_REQUEST, "date-in-past");
        } catch (LunchAlreadyPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-already-planned");
        }
    }

    @POST
    @Path("{id}/cancelled")
    public Response cancel(@PathParam("id") UUID id) {
        currentMember();
        try {
            lunches.cancel(id);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        }
    }

    @POST
    @Path("{id}/comments")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response comment(@PathParam("id") UUID id, CommentRequest request) {
        Member author = currentMember();
        try {
            LunchComment comment = lunches.comment(id, author, request == null ? null : request.text());
            return Response.status(Response.Status.CREATED)
                    .entity(CommentJson.of(comment))
                    .build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (CommentTextRequiredException e) {
            return error(Response.Status.BAD_REQUEST, "text-required");
        }
    }

    private Member currentMember() {
        return membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
    }

    private static String stateOf(Lunch lunch) {
        return lunch.state.name().toLowerCase(Locale.ROOT);
    }

    private static Response error(Response.Status status, String error) {
        return Response.status(status).entity(Map.of("error", error)).build();
    }
}
