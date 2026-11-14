package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

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

    /** Fails unless the Lunches are refused to someone who isn't logged in. */
    public void assertNotAccessibleWithoutLogin() {
        driver.assertNotAccessibleWithoutLogin();
    }

    private String idOf(String alias) {
        String id = idByAlias.get(alias);
        if (id == null) {
            throw new IllegalArgumentException("No Lunch planned with alias '" + alias + "'");
        }
        return id;
    }

    /** {@code today}, or a number of days from today in Basel such as {@code +7} or {@code -60}. */
    private static LocalDate date(String relative) {
        LocalDate today = LocalDate.now(BASEL);
        return "today".equals(relative) ? today : today.plusDays(Long.parseLong(relative));
    }
}
