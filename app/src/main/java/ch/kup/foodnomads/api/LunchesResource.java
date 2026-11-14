package ch.kup.foodnomads.api;

import ch.kup.foodnomads.lunches.AlreadyCandidateException;
import ch.kup.foodnomads.lunches.CandidatePickedException;
import ch.kup.foodnomads.lunches.CandidateVenue;
import ch.kup.foodnomads.lunches.CommentTextRequiredException;
import ch.kup.foodnomads.lunches.DateInPastException;
import ch.kup.foodnomads.lunches.DateNotPastException;
import ch.kup.foodnomads.lunches.ListedCandidate;
import ch.kup.foodnomads.lunches.Lunch;
import ch.kup.foodnomads.lunches.LunchAlreadyPlannedException;
import ch.kup.foodnomads.lunches.LunchComment;
import ch.kup.foodnomads.lunches.LunchNotDueException;
import ch.kup.foodnomads.lunches.LunchNotFoundException;
import ch.kup.foodnomads.lunches.LunchNotPlannedException;
import ch.kup.foodnomads.lunches.Lunches;
import ch.kup.foodnomads.lunches.NoVenueException;
import ch.kup.foodnomads.lunches.NotACandidateException;
import ch.kup.foodnomads.lunches.RsvpAnswer;
import ch.kup.foodnomads.lunches.VenuePickedException;
import ch.kup.foodnomads.lunches.VenueVisitedException;
import ch.kup.foodnomads.lunches.VoteDirection;
import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.VenueNotFoundException;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
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

/**
 * Planning, moving and cancelling Lunches, recording past ones, their Candidate Venues and Votes,
 * RSVPs, picking the Venue, marking a Lunch held, and their Lunch Comments.
 */
@Path("/api/lunches")
@Produces(MediaType.APPLICATION_JSON)
public class LunchesResource {

    /** A Lunch's Venue. A Planned Lunch has none until a Member picks one. */
    public record VenueJson(UUID id, String name) {
        /** Null for no Venue. */
        static VenueJson of(Venue venue) {
            return venue == null ? null : new VenueJson(venue.id, venue.name);
        }
    }

    /** {@code in} is how many Members are in. */
    public record LunchSummaryJson(UUID id, LocalDate date, String state, VenueJson venue, int in) {
        static LunchSummaryJson of(Lunch lunch) {
            return new LunchSummaryJson(
                    lunch.id, lunch.date, stateOf(lunch), VenueJson.of(lunch.venue), lunch.inCount());
        }
    }

    public record LunchDetailJson(
            UUID id,
            LocalDate date,
            String state,
            VenueJson venue,
            int in,
            List<CandidateJson> candidates,
            RsvpsJson rsvps,
            List<CommentJson> comments) {
        static LunchDetailJson of(Lunch lunch, List<ListedCandidate> candidates) {
            return new LunchDetailJson(
                    lunch.id,
                    lunch.date,
                    stateOf(lunch),
                    VenueJson.of(lunch.venue),
                    lunch.inCount(),
                    candidates.stream().map(CandidateJson::of).toList(),
                    RsvpsJson.of(lunch),
                    lunch.chat().stream().map(CommentJson::of).toList());
        }
    }

    /** The display names of the Members who are in and of those who are out, by name. */
    public record RsvpsJson(List<String> in, List<String> out) {
        static RsvpsJson of(Lunch lunch) {
            return new RsvpsJson(names(lunch, RsvpAnswer.IN), names(lunch, RsvpAnswer.OUT));
        }

        private static List<String> names(Lunch lunch, RsvpAnswer answer) {
            return lunch.answered(answer).stream().map(Member::displayName).toList();
        }
    }

    /**
     * A Candidate Venue. {@code visited} once a Lunch has been held there after it was put up.
     * {@code myVote} is the caller's own Vote, {@code up}, {@code down} or null.
     */
    public record CandidateJson(VenueJson venue, boolean visited, int up, int down, String myVote) {
        static CandidateJson of(ListedCandidate candidate) {
            return new CandidateJson(
                    VenueJson.of(candidate.venue()),
                    candidate.visited(),
                    candidate.up(),
                    candidate.down(),
                    candidate.myVote() == null
                            ? null
                            : candidate.myVote().name().toLowerCase(Locale.ROOT));
        }
    }

    public record CommentJson(UUID id, String author, String text, Instant writtenAt) {
        static CommentJson of(LunchComment comment) {
            return new CommentJson(comment.id, comment.author.displayName, comment.text, comment.writtenAt);
        }
    }

    public record PlanRequest(LocalDate date) {}

    public record RecordPastRequest(LocalDate date, UUID venueId) {}

    public record CommentRequest(String text) {}

    public record CandidateRequest(UUID venueId) {}

    /** {@code direction} is {@code up} or {@code down}. */
    public record VoteRequest(String direction) {}

    /** {@code answer} is {@code in} or {@code out}. */
    public record RsvpRequest(String answer) {}

    public record MoveRequest(LocalDate date) {}

    public record PickRequest(UUID venueId) {}

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
        Member caller = currentMember();
        try {
            Lunch lunch = lunches.lunch(id);
            return LunchDetailJson.of(lunch, lunches.candidates(lunch, caller));
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
    @Path("past")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response recordPast(RecordPastRequest request) {
        currentMember();
        if (request == null || request.date() == null) {
            return error(Response.Status.BAD_REQUEST, "date-required");
        }
        if (request.venueId() == null) {
            return error(Response.Status.BAD_REQUEST, "venue-required");
        }
        try {
            Lunch lunch = lunches.recordPast(request.date(), request.venueId());
            return Response.status(Response.Status.CREATED)
                    .entity(LunchSummaryJson.of(lunch))
                    .build();
        } catch (DateNotPastException e) {
            return error(Response.Status.BAD_REQUEST, "date-not-past");
        } catch (VenueNotFoundException e) {
            throw new NotFoundException(e);
        } catch (VenueVisitedException e) {
            return error(Response.Status.CONFLICT, "venue-visited");
        } catch (VenuePickedException e) {
            return error(Response.Status.CONFLICT, "venue-picked");
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

    @PUT
    @Path("{id}/date")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response move(@PathParam("id") UUID id, MoveRequest request) {
        currentMember();
        if (request == null || request.date() == null) {
            return error(Response.Status.BAD_REQUEST, "date-required");
        }
        try {
            lunches.move(id, request.date());
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (DateInPastException e) {
            return error(Response.Status.BAD_REQUEST, "date-in-past");
        }
    }

    @PUT
    @Path("{id}/rsvp")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response rsvp(@PathParam("id") UUID id, RsvpRequest request) {
        Member member = currentMember();
        RsvpAnswer answer = request == null ? null : answerOf(request.answer());
        if (answer == null) {
            return error(Response.Status.BAD_REQUEST, "answer-required");
        }
        try {
            lunches.rsvp(member, id, answer);
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

    @POST
    @Path("{id}/candidates")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCandidate(@PathParam("id") UUID id, CandidateRequest request) {
        currentMember();
        if (request == null || request.venueId() == null) {
            return error(Response.Status.BAD_REQUEST, "venue-required");
        }
        try {
            CandidateVenue candidate = lunches.addCandidate(id, request.venueId());
            return Response.status(Response.Status.CREATED)
                    .entity(new CandidateJson(VenueJson.of(candidate.venue), false, 0, 0, null))
                    .build();
        } catch (LunchNotFoundException | VenueNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (VenueVisitedException e) {
            return error(Response.Status.CONFLICT, "venue-visited");
        } catch (AlreadyCandidateException e) {
            return error(Response.Status.CONFLICT, "already-candidate");
        }
    }

    @DELETE
    @Path("{id}/candidates/{venueId}")
    public Response removeCandidate(@PathParam("id") UUID id, @PathParam("venueId") UUID venueId) {
        currentMember();
        try {
            lunches.removeCandidate(id, venueId);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (NotACandidateException e) {
            return error(Response.Status.CONFLICT, "not-a-candidate");
        } catch (CandidatePickedException e) {
            return error(Response.Status.CONFLICT, "candidate-picked");
        }
    }

    @PUT
    @Path("{id}/venue")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response pick(@PathParam("id") UUID id, PickRequest request) {
        currentMember();
        if (request == null || request.venueId() == null) {
            return error(Response.Status.BAD_REQUEST, "venue-required");
        }
        try {
            lunches.pick(id, request.venueId());
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (NotACandidateException e) {
            return error(Response.Status.CONFLICT, "not-a-candidate");
        } catch (VenueVisitedException e) {
            return error(Response.Status.CONFLICT, "venue-visited");
        }
    }

    @DELETE
    @Path("{id}/venue")
    public Response clearPick(@PathParam("id") UUID id) {
        currentMember();
        try {
            lunches.clearPick(id);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        }
    }

    @POST
    @Path("{id}/held")
    public Response hold(@PathParam("id") UUID id) {
        currentMember();
        try {
            lunches.hold(id);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (LunchNotDueException e) {
            return error(Response.Status.CONFLICT, "lunch-not-due");
        } catch (NoVenueException e) {
            return error(Response.Status.CONFLICT, "no-venue");
        } catch (VenueVisitedException e) {
            return error(Response.Status.CONFLICT, "venue-visited");
        }
    }

    @PUT
    @Path("{id}/candidates/{venueId}/vote")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response vote(@PathParam("id") UUID id, @PathParam("venueId") UUID venueId, VoteRequest request) {
        Member voter = currentMember();
        VoteDirection direction = request == null ? null : directionOf(request.direction());
        if (direction == null) {
            return error(Response.Status.BAD_REQUEST, "direction-required");
        }
        try {
            lunches.vote(voter, id, venueId, direction);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (NotACandidateException e) {
            return error(Response.Status.CONFLICT, "not-a-candidate");
        }
    }

    @DELETE
    @Path("{id}/candidates/{venueId}/vote")
    public Response withdrawVote(@PathParam("id") UUID id, @PathParam("venueId") UUID venueId) {
        Member voter = currentMember();
        try {
            lunches.withdrawVote(voter, id, venueId);
            return Response.noContent().build();
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            return error(Response.Status.CONFLICT, "lunch-not-planned");
        } catch (NotACandidateException e) {
            return error(Response.Status.CONFLICT, "not-a-candidate");
        }
    }

    /** {@code up} or {@code down} as a direction, or null for anything else. */
    private static VoteDirection directionOf(String direction) {
        if ("up".equals(direction)) {
            return VoteDirection.UP;
        }
        if ("down".equals(direction)) {
            return VoteDirection.DOWN;
        }
        return null;
    }

    /** {@code in} or {@code out} as an answer, or null for anything else. */
    private static RsvpAnswer answerOf(String answer) {
        if ("in".equals(answer)) {
            return RsvpAnswer.IN;
        }
        if ("out".equals(answer)) {
            return RsvpAnswer.OUT;
        }
        return null;
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
