package ch.kup.foodnomads.web;

import ch.kup.foodnomads.lunches.AlreadyCandidateException;
import ch.kup.foodnomads.lunches.CommentTextRequiredException;
import ch.kup.foodnomads.lunches.DateInPastException;
import ch.kup.foodnomads.lunches.DateNotPastException;
import ch.kup.foodnomads.lunches.ListedCandidate;
import ch.kup.foodnomads.lunches.Lunch;
import ch.kup.foodnomads.lunches.LunchAlreadyPlannedException;
import ch.kup.foodnomads.lunches.LunchComment;
import ch.kup.foodnomads.lunches.LunchNotFoundException;
import ch.kup.foodnomads.lunches.LunchNotPlannedException;
import ch.kup.foodnomads.lunches.Lunches;
import ch.kup.foodnomads.lunches.NotACandidateException;
import ch.kup.foodnomads.lunches.RsvpAnswer;
import ch.kup.foodnomads.lunches.VenueVisitedException;
import ch.kup.foodnomads.lunches.VoteDirection;
import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.Membership;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.VenueNotFoundException;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Lunches: planning one when none is planned, recording one from before the app, the list,
 * and a Lunch's own page with its date, who is in and out, its Candidate Venues, their Votes and its
 * chat. The forms are ordinary form posts that come back to a page, so a reload doesn't post again.
 */
@Path("/lunches")
@Produces(MediaType.TEXT_HTML)
public class LunchesPage {

    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private static final URI LIST = URI.create("/lunches");

    /** {@code venue} is the Venue's name, or null for none yet. {@code in} is how many Members are in. */
    public record LunchRow(UUID id, String date, String dateOn, String state, boolean planned, String venue, int in) {
        static LunchRow of(Lunch lunch) {
            return new LunchRow(
                    lunch.id,
                    lunch.date.toString(),
                    VenuesPage.DATE.format(lunch.date),
                    lunch.state.name().toLowerCase(Locale.ROOT),
                    lunch.isPlanned(),
                    lunch.venue == null ? null : lunch.venue.name,
                    lunch.inCount());
        }
    }

    /**
     * Who is in and who is out, by display name. {@code mine} is the Member's own answer, {@code in},
     * {@code out} or empty for none.
     */
    public record Rsvps(List<String> in, List<String> out, String mine) {
        static Rsvps of(Lunch lunch, Member viewer) {
            RsvpAnswer mine = lunch.answerOf(viewer);
            return new Rsvps(
                    names(lunch, RsvpAnswer.IN),
                    names(lunch, RsvpAnswer.OUT),
                    mine == null ? "" : mine.name().toLowerCase(Locale.ROOT));
        }

        private static List<String> names(Lunch lunch, RsvpAnswer answer) {
            return lunch.answered(answer).stream().map(Member::displayName).toList();
        }
    }

    /** A Venue a past Lunch can be recorded at, or a Candidate Venue can be put up from. */
    public record VenueOption(String id, String name) {
        static VenueOption of(Venue venue) {
            return new VenueOption(venue.id.toString(), venue.name);
        }
    }

    /**
     * A Candidate Venue, marked visited once a Lunch has been held there, with its up and down
     * Votes. {@code myVote} is the Member's own, {@code up}, {@code down} or empty for none.
     */
    public record CandidateRow(String venueId, String name, boolean visited, int up, int down, String myVote) {
        static CandidateRow of(ListedCandidate candidate) {
            return new CandidateRow(
                    candidate.venue().id.toString(),
                    candidate.venue().name,
                    candidate.visited(),
                    candidate.up(),
                    candidate.down(),
                    candidate.myVote() == null ? "" : candidate.myVote().name().toLowerCase(Locale.ROOT));
        }
    }

    public record Comment(String author, String text, String writtenAt, String writtenOn) {
        static Comment of(LunchComment comment) {
            var writtenAt = comment.writtenAt.atZone(VenuesPage.ZONE);
            return new Comment(
                    comment.author.displayName,
                    comment.text,
                    writtenAt.toOffsetDateTime().toString(),
                    DATE_TIME.format(writtenAt));
        }
    }

    /** What the plan form shows. {@code error} is null, or which rule refused the date. */
    public record PlanDraft(String date, String error) {
        static final PlanDraft EMPTY = new PlanDraft("", null);
    }

    /** What the record form shows. {@code error} is null, or which rule refused the Lunch. */
    public record RecordDraft(String date, String venueId, String error) {
        static final RecordDraft EMPTY = new RecordDraft("", "", null);
    }

    /** What the form to move the Lunch shows. {@code error} is null, or which rule refused the date. */
    public record MoveDraft(String date, String error) {
        static final MoveDraft EMPTY = new MoveDraft("", null);
    }

    /** The form to move the Lunch: {@code today} is the earliest date it takes. */
    public record MoveForm(String today, MoveDraft draft) {}

    /** What the form to put up a Candidate Venue shows. {@code error} is null, or which rule refused it. */
    public record CandidateDraft(String venueId, String error) {
        static final CandidateDraft EMPTY = new CandidateDraft("", null);
    }

    /** The form to put up a Candidate Venue: {@code addable} are the Venues it offers, none unless the Lunch is planned. */
    public record CandidateForm(List<VenueOption> addable, CandidateDraft draft) {}

    /** What the comment form shows after a refused comment. */
    public record CommentDraft(String text, boolean rejected) {
        static final CommentDraft EMPTY = new CommentDraft("", false);
    }

    @CheckedTemplate
    static class Templates {
        /**
         * @param planned the Planned Lunch, or null if there is none
         * @param venues the unvisited Venues, which a past Lunch can be recorded at
         */
        static native TemplateInstance lunches(
                LunchRow planned,
                List<LunchRow> others,
                String today,
                PlanDraft draft,
                List<VenueOption> venues,
                String yesterday,
                RecordDraft pastDraft);

        static native TemplateInstance lunch(
                LunchRow lunch,
                MoveForm move,
                Rsvps rsvps,
                List<CandidateRow> candidates,
                CandidateForm candidateForm,
                List<Comment> comments,
                CommentDraft draft);
    }

    @Inject
    Lunches lunches;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public TemplateInstance list() {
        return listPage(PlanDraft.EMPTY, RecordDraft.EMPTY);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response plan(@FormParam("date") String date) {
        currentMember();
        String given = date == null ? "" : date.strip();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(given);
        } catch (DateTimeParseException e) {
            return Response.status(422)
                    .entity(listPage(new PlanDraft(given, "date-required"), RecordDraft.EMPTY))
                    .build();
        }
        try {
            Lunch lunch = lunches.plan(parsed);
            return Response.seeOther(lunchPath(lunch.id)).build();
        } catch (DateInPastException e) {
            return Response.status(422)
                    .entity(listPage(new PlanDraft(given, "date-in-past"), RecordDraft.EMPTY))
                    .build();
        } catch (LunchAlreadyPlannedException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(listPage(new PlanDraft(given, "already-planned"), RecordDraft.EMPTY))
                    .build();
        }
    }

    @POST
    @Path("past")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response recordPast(@FormParam("date") String date, @FormParam("venue") String venue) {
        currentMember();
        String givenDate = date == null ? "" : date.strip();
        String givenVenue = venue == null ? "" : venue.strip();
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(givenDate);
        } catch (DateTimeParseException e) {
            return recordRefused(422, givenDate, givenVenue, "date-required");
        }
        UUID venueId;
        try {
            venueId = UUID.fromString(givenVenue);
        } catch (IllegalArgumentException e) {
            return recordRefused(422, givenDate, givenVenue, "venue-required");
        }
        try {
            Lunch lunch = lunches.recordPast(parsedDate, venueId);
            return Response.seeOther(lunchPath(lunch.id)).build();
        } catch (DateNotPastException e) {
            return recordRefused(422, givenDate, givenVenue, "date-not-past");
        } catch (VenueNotFoundException e) {
            throw new NotFoundException(e);
        } catch (VenueVisitedException e) {
            return recordRefused(Response.Status.CONFLICT.getStatusCode(), givenDate, givenVenue, "venue-visited");
        }
    }

    private Response recordRefused(int status, String date, String venueId, String error) {
        return Response.status(status)
                .entity(listPage(PlanDraft.EMPTY, new RecordDraft(date, venueId, error)))
                .build();
    }

    @GET
    @Path("{id}")
    public TemplateInstance lunch(@PathParam("id") UUID id) {
        return lunchPage(id, MoveDraft.EMPTY, CandidateDraft.EMPTY, CommentDraft.EMPTY);
    }

    @POST
    @Path("{id}/cancel")
    public Response cancel(@PathParam("id") UUID id) {
        currentMember();
        try {
            lunches.cancel(id);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            // Someone else was quicker. The page shows how things stand.
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    @POST
    @Path("{id}/date")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response move(@PathParam("id") UUID id, @FormParam("date") String date) {
        currentMember();
        String given = date == null ? "" : date.strip();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(given);
        } catch (DateTimeParseException e) {
            return moveRefused(id, given, "date-required");
        }
        try {
            lunches.move(id, parsed);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            // Someone else was quicker. The page shows how things stand.
        } catch (DateInPastException e) {
            return moveRefused(id, given, "date-in-past");
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    private Response moveRefused(UUID id, String date, String error) {
        return Response.status(422)
                .entity(lunchPage(id, new MoveDraft(date, error), CandidateDraft.EMPTY, CommentDraft.EMPTY))
                .build();
    }

    /** Answers {@code in} or {@code out}; anything else just comes back to the page. */
    @POST
    @Path("{id}/rsvp")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response rsvp(@PathParam("id") UUID id, @FormParam("answer") String answer) {
        Member member = currentMember();
        try {
            if ("in".equals(answer)) {
                lunches.rsvp(member, id, RsvpAnswer.IN);
            } else if ("out".equals(answer)) {
                lunches.rsvp(member, id, RsvpAnswer.OUT);
            }
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            // Someone else was quicker. The page shows how things stand.
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    @POST
    @Path("{id}/comments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response comment(@PathParam("id") UUID id, @FormParam("text") String text) {
        Member author = currentMember();
        try {
            lunches.comment(id, author, text);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (CommentTextRequiredException e) {
            return Response.status(422)
                    .entity(lunchPage(
                            id,
                            MoveDraft.EMPTY,
                            CandidateDraft.EMPTY,
                            new CommentDraft(text == null ? "" : text, true)))
                    .build();
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    @POST
    @Path("{id}/candidates")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addCandidate(@PathParam("id") UUID id, @FormParam("venue") String venue) {
        currentMember();
        String given = venue == null ? "" : venue.strip();
        UUID venueId;
        try {
            venueId = UUID.fromString(given);
        } catch (IllegalArgumentException e) {
            return candidateRefused(422, id, given, "venue-required");
        }
        try {
            lunches.addCandidate(id, venueId);
        } catch (LunchNotFoundException | VenueNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException e) {
            // Someone else was quicker. The page shows how things stand.
        } catch (VenueVisitedException e) {
            return candidateRefused(Response.Status.CONFLICT.getStatusCode(), id, given, "venue-visited");
        } catch (AlreadyCandidateException e) {
            return candidateRefused(Response.Status.CONFLICT.getStatusCode(), id, given, "already-candidate");
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    @POST
    @Path("{id}/candidates/{venueId}/remove")
    public Response removeCandidate(@PathParam("id") UUID id, @PathParam("venueId") UUID venueId) {
        currentMember();
        try {
            lunches.removeCandidate(id, venueId);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException | NotACandidateException e) {
            // Someone else was quicker. The page shows how things stand.
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    /** Votes {@code up} or {@code down}; anything else just comes back to the page. */
    @POST
    @Path("{id}/candidates/{venueId}/vote")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response vote(
            @PathParam("id") UUID id, @PathParam("venueId") UUID venueId, @FormParam("direction") String direction) {
        Member voter = currentMember();
        try {
            if ("up".equals(direction)) {
                lunches.vote(voter, id, venueId, VoteDirection.UP);
            } else if ("down".equals(direction)) {
                lunches.vote(voter, id, venueId, VoteDirection.DOWN);
            }
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException | NotACandidateException e) {
            // Someone else was quicker. The page shows how things stand.
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    @POST
    @Path("{id}/candidates/{venueId}/vote/withdraw")
    public Response withdrawVote(@PathParam("id") UUID id, @PathParam("venueId") UUID venueId) {
        Member voter = currentMember();
        try {
            lunches.withdrawVote(voter, id, venueId);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        } catch (LunchNotPlannedException | NotACandidateException e) {
            // Someone else was quicker. The page shows how things stand.
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    private Response candidateRefused(int status, UUID id, String venueId, String error) {
        return Response.status(status)
                .entity(lunchPage(id, MoveDraft.EMPTY, new CandidateDraft(venueId, error), CommentDraft.EMPTY))
                .build();
    }

    private Member currentMember() {
        return membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
    }

    private TemplateInstance listPage(PlanDraft draft, RecordDraft pastDraft) {
        List<LunchRow> all = lunches.all().stream().map(LunchRow::of).toList();
        LunchRow planned = all.stream().filter(LunchRow::planned).findFirst().orElse(null);
        List<LunchRow> others = all.stream().filter(row -> !row.planned()).toList();
        LocalDate today = lunches.today();
        return Templates.lunches(
                planned,
                others,
                today.toString(),
                draft,
                lunches.unvisitedVenues().stream().map(VenueOption::of).toList(),
                today.minusDays(1).toString(),
                pastDraft);
    }

    private TemplateInstance lunchPage(
            UUID id, MoveDraft moveDraft, CandidateDraft candidateDraft, CommentDraft draft) {
        Member viewer = currentMember();
        Lunch lunch;
        try {
            lunch = lunches.lunch(id);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        }
        return Templates.lunch(
                LunchRow.of(lunch),
                new MoveForm(lunches.today().toString(), moveDraft),
                Rsvps.of(lunch, viewer),
                lunches.candidates(lunch, viewer).stream().map(CandidateRow::of).toList(),
                new CandidateForm(
                        lunches.addableVenues(lunch).stream()
                                .map(VenueOption::of)
                                .toList(),
                        candidateDraft),
                lunch.chat().stream().map(Comment::of).toList(),
                draft);
    }

    static URI lunchPath(UUID id) {
        return URI.create(LIST + "/" + id);
    }
}
