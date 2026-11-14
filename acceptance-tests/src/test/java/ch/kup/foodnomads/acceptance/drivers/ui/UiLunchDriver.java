package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

class UiLunchDriver implements LunchDriver {

    private static final Pattern LUNCH_PAGE = Pattern.compile(".*/lunches/([0-9a-f-]{36})$");

    private final Page page;
    private final URI baseUrl;

    UiLunchDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public String plan(LocalDate date) {
        submitPlan(date);

        return lunchIdFromPage();
    }

    @Override
    public void assertPlanRejectedAsInThePast(LocalDate date) {
        submitPlan(date);

        assertThat(page.locator("#plan-date-in-past")).isVisible();
        assertThat(page.locator("main#lunch")).not().isAttached();
    }

    /** The page offers no form while a Lunch is planned, but shows that one instead. */
    @Override
    public void assertPlanRejectedAsAlreadyPlanned(LocalDate date) {
        page.navigate(baseUrl.resolve("lunches").toString());

        assertThat(page.locator("#planned-lunch")).isVisible();
        assertThat(page.locator("#plan-form")).not().isAttached();
    }

    @Override
    public void cancelPlannedLunch() {
        page.navigate(baseUrl.resolve("lunches").toString());
        assertThat(page.locator("main#lunches")).isVisible();
        if (page.locator("#planned-lunch").count() == 0) {
            return;
        }
        page.locator("#planned-lunch").click();
        cancelOnLunchPage();
    }

    @Override
    public void assertPlanned(String lunchId, LocalDate date, String venue) {
        page.navigate(baseUrl.toString());

        Locator planned = page.locator("#planned-lunch");
        assertThat(planned).hasAttribute("href", "/lunches/" + lunchId);
        assertThat(planned.locator(".lunch-date")).hasAttribute("datetime", date.toString());
        assertVenue(planned.locator(".lunch-venue"), venue);

        planned.click();
        assertThat(page.locator("#lunch-date")).hasAttribute("datetime", date.toString());
        assertThat(page.locator("#lunch-state")).hasAttribute("data-state", "planned");
        if (venue == null) {
            assertThat(page.locator("#lunch-no-venue")).isVisible();
        } else {
            assertThat(page.locator("#lunch-venue")).hasText(venue);
        }
    }

    @Override
    public void cancel(String lunchId) {
        page.navigate(lunchPage(lunchId));
        cancelOnLunchPage();
    }

    @Override
    public void assertCancelled(String lunchId) {
        page.navigate(baseUrl.resolve("lunches").toString());

        Locator listed = page.locator("#earlier-lunches .lunch:has(.lunch-link[href='/lunches/" + lunchId + "'])");
        assertThat(listed.locator(".lunch-state")).hasAttribute("data-state", "cancelled");

        listed.locator(".lunch-link").click();
        assertThat(page.locator("#lunch-state")).hasAttribute("data-state", "cancelled");
        assertThat(page.locator("#cancel-lunch-button")).not().isAttached();
    }

    @Override
    public void comment(String lunchId, String text) {
        submitComment(lunchId, text);

        assertThat(page.locator("#comment-error")).not().isAttached();
        assertThat(page.locator("#comment-text")).hasValue("");
    }

    @Override
    public void assertCommentRejectedAsBlank(String lunchId, String text) {
        submitComment(lunchId, text);

        assertThat(page.locator("#comment-error")).isVisible();
        assertThat(page.locator(".lunch-comment")).hasCount(0);
    }

    @Override
    public void assertComment(String lunchId, String author, String text) {
        page.navigate(lunchPage(lunchId));

        Locator comment = page.locator("#lunch-comments .lunch-comment")
                .filter(new Locator.FilterOptions().setHas(exactly(".comment-author", author)))
                .filter(new Locator.FilterOptions().setHas(exactly(".comment-text", text)));
        assertThat(comment).hasCount(1);
        assertThat(comment.locator("time")).isVisible();
    }

    @Override
    public String recordPast(LocalDate date, String venue) {
        submitRecordPast(date, venue);

        return lunchIdFromPage();
    }

    /** The form only offers unvisited Venues, so a Visited Venue can't be chosen at all. */
    @Override
    public void assertRecordPastRejectedAsVenueVisited(LocalDate date, String venue) {
        page.navigate(baseUrl.resolve("lunches").toString());
        assertThat(page.locator("main#lunches")).isVisible();

        assertThat(page.locator("#record-venue option").filter(new Locator.FilterOptions().setHasText(venue)))
                .hasCount(0);
    }

    @Override
    public void assertRecordPastRejectedAsDateNotPast(LocalDate date, String venue) {
        submitRecordPast(date, venue);

        assertThat(page.locator("#record-date-not-past")).isVisible();
        assertThat(page.locator("main#lunch")).not().isAttached();
    }

    @Override
    public void assertHeld(String lunchId, LocalDate date, String venue) {
        page.navigate(baseUrl.resolve("lunches").toString());

        Locator listed = page.locator("#earlier-lunches .lunch:has(.lunch-link[href='/lunches/" + lunchId + "'])");
        assertThat(listed.locator(".lunch-state")).hasAttribute("data-state", "held");
        assertThat(listed.locator(".lunch-date")).hasAttribute("datetime", date.toString());
        assertThat(listed.locator(".lunch-venue")).hasText(venue);

        listed.locator(".lunch-link").click();
        assertThat(page.locator("#lunch-date")).hasAttribute("datetime", date.toString());
        assertThat(page.locator("#lunch-state")).hasAttribute("data-state", "held");
        assertThat(page.locator("#lunch-venue")).hasText(venue);
        assertThat(page.locator("#cancel-lunch-button")).not().isAttached();
        assertThat(page.locator("#comment-form")).isVisible();
    }

    @Override
    public void addCandidate(String lunchId, String venue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertAddCandidateRejectedAsVenueVisited(String lunchId, String venue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void removeCandidate(String lunchId, String venue) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertCandidates(String lunchId, Set<String> venues, Set<String> visited) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        page.navigate(baseUrl.resolve("lunches").toString());

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        assertThat(page.locator("main#login")).isVisible();
        assertThat(page.locator("main#lunches")).not().isAttached();
    }

    private void submitPlan(LocalDate date) {
        page.navigate(baseUrl.toString());
        page.locator("#lunches-link").click();
        page.locator("#lunch-date").fill(date.toString());
        page.locator("#plan-button").click();
    }

    private void submitRecordPast(LocalDate date, String venue) {
        page.navigate(baseUrl.resolve("lunches").toString());
        page.locator("#record-open").click();
        page.locator("#record-date").fill(date.toString());
        page.locator("#record-venue").selectOption(new SelectOption().setLabel(venue));
        page.locator("#record-button").click();
    }

    /** Waits for a Lunch's page and returns the Lunch's id from its address. */
    private String lunchIdFromPage() {
        page.waitForURL(LUNCH_PAGE);
        assertThat(page.locator("main#lunch")).isVisible();
        var matcher = LUNCH_PAGE.matcher(page.url());
        if (!matcher.matches()) {
            throw new AssertionError("Not on a Lunch page: " + page.url());
        }
        return matcher.group(1);
    }

    private void submitComment(String lunchId, String text) {
        page.navigate(lunchPage(lunchId));
        page.locator("#comment-text").fill(text);
        page.waitForResponse(
                r -> r.url().endsWith("/lunches/" + lunchId + "/comments")
                        && "POST".equals(r.request().method()),
                () -> page.locator("#comment-button").click());
    }

    /** Cancels the Lunch on the page shown, answering yes when asked to confirm. */
    private void cancelOnLunchPage() {
        page.onceDialog(Dialog::accept);
        page.locator("#cancel-lunch-button").click();

        assertThat(page.locator("#lunch-state")).hasAttribute("data-state", "cancelled");
    }

    private void assertVenue(Locator shown, String venue) {
        if (venue == null) {
            assertThat(shown).hasClass(Pattern.compile("\\bno-venue\\b"));
        } else {
            assertThat(shown).hasText(venue);
        }
    }

    private Locator exactly(String selector, String text) {
        return page.locator(selector).getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    private String lunchPage(String lunchId) {
        return baseUrl.resolve("lunches/" + lunchId).toString();
    }
}
