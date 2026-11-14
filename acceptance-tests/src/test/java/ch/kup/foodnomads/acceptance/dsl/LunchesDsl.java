package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DSL for Lunches. A Lunch alias stands for the id the driver got back when planning it. Dates
 * are relative to today in Basel: {@code today}, {@code +7}, {@code -60}. Venue names share their
 * aliases with {@link VenuesDsl}.
 *
 * <p>The Planned Lunch is shared by all tests, so aliases can't isolate it. A test that plans a
 * Lunch starts with {@link #noPlannedLunch()}, which works because the tests run one after
 * another.
 */
public class LunchesDsl {

    /** The group lunches in Basel, so that's where "today" is. */
    private static final ZoneId BASEL = ZoneId.of("Europe/Zurich");

    private static final String NO_VENUE = "none";

    private final LunchDriver driver;
    private final Aliases venueAliases;
    private final MembersDsl members;
    private final Map<String, String> idByAlias = new HashMap<>();

    LunchesDsl(LunchDriver driver, Aliases venueAliases, MembersDsl members) {
        this.driver = driver;
        this.venueAliases = venueAliases;
        this.members = members;
    }

    /** Cancels whatever Lunch is planned, so the test can plan its own. */
    public void noPlannedLunch() {
        driver.cancelPlannedLunch();
    }

    /**
     * Plans a Lunch as the logged-in Member and remembers it under {@code lunch}. With
     * {@code expect: dateInPast} or {@code expect: alreadyPlanned}, fails unless planning is
     * refused for that reason.
     */
    public void plan(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("date"), Params.optional("expect", "success"));
        LocalDate date = date(params.value("date"));
        switch (params.value("expect")) {
            case "success" -> idByAlias.put(params.value("lunch"), driver.plan(date));
            case "dateInPast" -> driver.assertPlanRejectedAsInThePast(date);
            case "alreadyPlanned" -> driver.assertPlanRejectedAsAlreadyPlanned(date);
            default ->
                throw new IllegalArgumentException("Unknown expect '" + params.value("expect")
                        + "', expected success, dateInPast or alreadyPlanned");
        }
    }

    /** Fails unless {@code lunch} is the Planned Lunch, on {@code date}, with {@code venue} ({@code none} for none). */
    public void assertPlanned(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("date"), Params.optional("venue", NO_VENUE));
        String venue = params.value("venue");
        driver.assertPlanned(
                idOf(params.value("lunch")),
                date(params.value("date")),
                NO_VENUE.equals(venue) ? null : venueAliases.realName(venue));
    }

    /** Cancels {@code lunch} as the logged-in Member. */
    public void cancel(String... args) {
        Params params = Params.parse(args, Params.required("lunch"));
        driver.cancel(idOf(params.value("lunch")));
    }

    /** Fails unless {@code lunch} is a Cancelled Lunch. */
    public void assertCancelled(String... args) {
        Params params = Params.parse(args, Params.required("lunch"));
        driver.assertCancelled(idOf(params.value("lunch")));
    }

    /**
     * Comments on {@code lunch} as the logged-in Member. With {@code expect: textRequired}, fails
     * unless the comment is refused for having no text.
     */
    public void comment(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("text"), Params.optional("expect", "success"));
        String id = idOf(params.value("lunch"));
        String text = params.value("text");
        switch (params.value("expect")) {
            case "success" -> driver.comment(id, text);
            case "textRequired" -> driver.assertCommentRejectedAsBlank(id, text);
            default ->
                throw new IllegalArgumentException(
                        "Unknown expect '" + params.value("expect") + "', expected success or textRequired");
        }
    }

    /** Fails unless {@code lunch} shows a comment by the Member {@code by} with {@code text}. */
    public void assertComment(String... args) {
        Params params = Params.parse(args, Params.required("lunch"), Params.required("by"), Params.required("text"));
        driver.assertComment(idOf(params.value("lunch")), members.realName(params.value("by")), params.value("text"));
    }

    /**
     * Records a Lunch from before the app as the logged-in Member, on {@code date} at
     * {@code venue}, and remembers it under {@code lunch}. With {@code expect: venueVisited} or
     * {@code expect: dateNotPast}, fails unless recording is refused for that reason.
     */
    public void recordPast(String... args) {
        Params params = Params.parse(
                args,
                Params.required("lunch"),
                Params.required("date"),
                Params.required("venue"),
                Params.optional("expect", "success"));
        LocalDate date = date(params.value("date"));
        String venue = venueAliases.realName(params.value("venue"));
        switch (params.value("expect")) {
            case "success" -> idByAlias.put(params.value("lunch"), driver.recordPast(date, venue));
            case "venueVisited" -> driver.assertRecordPastRejectedAsVenueVisited(date, venue);
            case "dateNotPast" -> driver.assertRecordPastRejectedAsDateNotPast(date, venue);
            default ->
                throw new IllegalArgumentException("Unknown expect '" + params.value("expect")
                        + "', expected success, venueVisited or dateNotPast");
        }
    }

    /** Fails unless {@code lunch} is a Held Lunch, on {@code date}, at {@code venue}. */
    public void assertHeld(String... args) {
        Params params = Params.parse(args, Params.required("lunch"), Params.required("date"), Params.required("venue"));
        driver.assertHeld(
                idOf(params.value("lunch")), date(params.value("date")), venueAliases.realName(params.value("venue")));
    }

    /**
     * Puts up {@code venue} as a Candidate Venue of {@code lunch}, as the logged-in Member. With
     * {@code expect: venueVisited}, fails unless it is refused because the Venue is visited.
     */
    public void addCandidate(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("venue"), Params.optional("expect", "success"));
        String id = idOf(params.value("lunch"));
        String venue = venueAliases.realName(params.value("venue"));
        switch (params.value("expect")) {
            case "success" -> driver.addCandidate(id, venue);
            case "venueVisited" -> driver.assertAddCandidateRejectedAsVenueVisited(id, venue);
            default ->
                throw new IllegalArgumentException(
                        "Unknown expect '" + params.value("expect") + "', expected success or venueVisited");
        }
    }

    /** Removes the Candidate Venue {@code venue} from {@code lunch}, as the logged-in Member. */
    public void removeCandidate(String... args) {
        Params params = Params.parse(args, Params.required("lunch"), Params.required("venue"));
        driver.removeCandidate(idOf(params.value("lunch")), venueAliases.realName(params.value("venue")));
    }

    /**
     * Fails unless {@code lunch} lists exactly the Candidate Venues in {@code venues}
     * (comma-separated, in any order), and marks exactly those in {@code visited} as visited.
     */
    public void assertCandidates(String... args) {
        Params params =
                Params.parse(args, Params.required("lunch"), Params.required("venues"), Params.optional("visited", ""));
        driver.assertCandidates(
                idOf(params.value("lunch")), venueNames(params.value("venues")), venueNames(params.value("visited")));
    }

    /**
     * Votes {@code direction} ({@code up} or {@code down}) on the Candidate Venue {@code venue} of
     * {@code lunch}, as the logged-in Member.
     */
    public void vote(String... args) {
        Params params =
                Params.parse(args, Params.required("lunch"), Params.required("venue"), Params.required("direction"));
        String direction = params.value("direction");
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new IllegalArgumentException("Unknown direction '" + direction + "', expected up or down");
        }
        driver.vote(idOf(params.value("lunch")), venueAliases.realName(params.value("venue")), direction);
    }

    /** Withdraws the logged-in Member's Vote on the Candidate Venue {@code venue} of {@code lunch}. */
    public void withdrawVote(String... args) {
        Params params = Params.parse(args, Params.required("lunch"), Params.required("venue"));
        driver.withdrawVote(idOf(params.value("lunch")), venueAliases.realName(params.value("venue")));
    }

    /** Fails unless the Candidate Venue {@code venue} of {@code lunch} shows {@code up} and {@code down} Votes. */
    public void assertVotes(String... args) {
        Params params = Params.parse(
                args,
                Params.required("lunch"),
                Params.required("venue"),
                Params.required("up"),
                Params.required("down"));
        driver.assertVotes(
                idOf(params.value("lunch")),
                venueAliases.realName(params.value("venue")),
                Integer.parseInt(params.value("up")),
                Integer.parseInt(params.value("down")));
    }

    /**
     * Answers {@code in} or {@code out} for {@code lunch}, as the logged-in Member. With
     * {@code expect: notPlanned}, fails unless the answer is refused because the Lunch isn't planned.
     */
    public void rsvp(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("answer"), Params.optional("expect", "success"));
        String id = idOf(params.value("lunch"));
        String answer = params.value("answer");
        if (!"in".equals(answer) && !"out".equals(answer)) {
            throw new IllegalArgumentException("Unknown answer '" + answer + "', expected in or out");
        }
        switch (params.value("expect")) {
            case "success" -> driver.rsvp(id, answer);
            case "notPlanned" -> driver.assertRsvpRejectedAsNotPlanned(id, answer);
            default ->
                throw new IllegalArgumentException(
                        "Unknown expect '" + params.value("expect") + "', expected success or notPlanned");
        }
    }

    /**
     * Fails unless {@code lunch} shows exactly the Members in {@code in} as in and those in
     * {@code out} as out (comma-separated Member names, in any order, empty for nobody).
     */
    public void assertRsvps(String... args) {
        Params params = Params.parse(args, Params.required("lunch"), Params.required("in"), Params.required("out"));
        driver.assertRsvps(
                idOf(params.value("lunch")), memberNames(params.value("in")), memberNames(params.value("out")));
    }

    /**
     * Moves {@code lunch} to {@code date}, as the logged-in Member. With {@code expect: dateInPast},
     * fails unless the move is refused because the date is before today.
     */
    public void move(String... args) {
        Params params = Params.parse(
                args, Params.required("lunch"), Params.required("date"), Params.optional("expect", "success"));
        String id = idOf(params.value("lunch"));
        LocalDate date = date(params.value("date"));
        switch (params.value("expect")) {
            case "success" -> driver.move(id, date);
            case "dateInPast" -> driver.assertMoveRejectedAsInThePast(id, date);
            default ->
                throw new IllegalArgumentException(
                        "Unknown expect '" + params.value("expect") + "', expected success or dateInPast");
        }
    }

    /** Fails unless the Lunches are refused to someone who isn't logged in. */
    public void assertNotAccessibleWithoutLogin() {
        driver.assertNotAccessibleWithoutLogin();
    }

    private String idOf(String alias) {
        String id = idByAlias.get(alias);
        if (id == null) {
            throw new IllegalArgumentException("No Lunch planned or recorded with alias '" + alias + "'");
        }
        return id;
    }

    /** The real names of these comma-separated Venue aliases. */
    private Set<String> venueNames(String aliases) {
        return Arrays.stream(aliases.split(","))
                .map(String::strip)
                .filter(alias -> !alias.isEmpty())
                .map(venueAliases::realName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** The real display names of these comma-separated Member names. */
    private Set<String> memberNames(String names) {
        return Arrays.stream(names.split(","))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .map(members::realName)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** {@code today}, or a number of days from today in Basel such as {@code +7} or {@code -60}. */
    private static LocalDate date(String relative) {
        LocalDate today = LocalDate.now(BASEL);
        return "today".equals(relative) ? today : today.plusDays(Long.parseLong(relative));
    }
}
