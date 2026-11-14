package ch.kup.foodnomads.web;

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
 * The Lunches: planning one when none is planned, the list, and a Lunch's own page with its chat.
 * The forms are ordinary form posts that come back to a page, so a reload doesn't post again.
 */
@Path("/lunches")
@Produces(MediaType.TEXT_HTML)
public class LunchesPage {

    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

    private static final URI LIST = URI.create("/lunches");

    public record LunchRow(UUID id, String date, String dateOn, String state, boolean planned) {
        static LunchRow of(Lunch lunch) {
            return new LunchRow(
                    lunch.id,
                    lunch.date.toString(),
                    VenuesPage.DATE.format(lunch.date),
                    lunch.state.name().toLowerCase(Locale.ROOT),
                    lunch.isPlanned());
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

    /** What the comment form shows after a refused comment. */
    public record CommentDraft(String text, boolean rejected) {
        static final CommentDraft EMPTY = new CommentDraft("", false);
    }

    @CheckedTemplate
    static class Templates {
        /** @param planned the Planned Lunch, or null if there is none */
        static native TemplateInstance lunches(LunchRow planned, List<LunchRow> others, String today, PlanDraft draft);

        static native TemplateInstance lunch(LunchRow lunch, List<Comment> comments, CommentDraft draft);
    }

    @Inject
    Lunches lunches;

    @Inject
    Membership membership;

    @Inject
    SecurityIdentity identity;

    @GET
    public TemplateInstance list() {
        return listPage(PlanDraft.EMPTY);
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
                    .entity(listPage(new PlanDraft(given, "date-required")))
                    .build();
        }
        try {
            Lunch lunch = lunches.plan(parsed);
            return Response.seeOther(lunchPath(lunch.id)).build();
        } catch (DateInPastException e) {
            return Response.status(422)
                    .entity(listPage(new PlanDraft(given, "date-in-past")))
                    .build();
        } catch (LunchAlreadyPlannedException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(listPage(new PlanDraft(given, "already-planned")))
                    .build();
        }
    }

    @GET
    @Path("{id}")
    public TemplateInstance lunch(@PathParam("id") UUID id) {
        return lunchPage(id, CommentDraft.EMPTY);
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
                    .entity(lunchPage(id, new CommentDraft(text == null ? "" : text, true)))
                    .build();
        }
        return Response.seeOther(lunchPath(id)).build();
    }

    private Member currentMember() {
        return membership.current(identity).orElseThrow(() -> new NotAuthorizedException("Member"));
    }

    private TemplateInstance listPage(PlanDraft draft) {
        List<LunchRow> all = lunches.all().stream().map(LunchRow::of).toList();
        LunchRow planned = all.stream().filter(LunchRow::planned).findFirst().orElse(null);
        List<LunchRow> others = all.stream().filter(row -> !row.planned()).toList();
        return Templates.lunches(planned, others, lunches.today().toString(), draft);
    }

    private TemplateInstance lunchPage(UUID id, CommentDraft draft) {
        Lunch lunch;
        try {
            lunch = lunches.lunch(id);
        } catch (LunchNotFoundException e) {
            throw new NotFoundException(e);
        }
        return Templates.lunch(
                LunchRow.of(lunch), lunch.chat().stream().map(Comment::of).toList(), draft);
    }

    static URI lunchPath(UUID id) {
        return URI.create(LIST + "/" + id);
    }
}
