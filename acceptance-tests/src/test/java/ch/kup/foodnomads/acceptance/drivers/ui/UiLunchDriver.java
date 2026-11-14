package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.time.LocalDate;

class UiLunchDriver implements LunchDriver {

    private final Page page;
    private final URI baseUrl;

    UiLunchDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public String plan(LocalDate date) {
        throw new UnsupportedOperationException("Not implemented yet: plans a Lunch on the lunches page");
    }

    @Override
    public void assertPlanRejectedAsInThePast(LocalDate date) {
        throw new UnsupportedOperationException(
                "Not implemented yet: planning before today is refused on the lunches page");
    }

    @Override
    public void assertPlanRejectedAsAlreadyPlanned(LocalDate date) {
        throw new UnsupportedOperationException(
                "Not implemented yet: only one Lunch is planned at a time on the lunches page");
    }

    @Override
    public void cancelPlannedLunch() {
        throw new UnsupportedOperationException(
                "Not implemented yet: the Planned Lunch can be cancelled on the lunches page");
    }

    @Override
    public void assertPlanned(String lunchId, LocalDate date, String venue) {
        throw new UnsupportedOperationException("Not implemented yet: the Planned Lunch is shown on the lunches page");
    }

    @Override
    public void cancel(String lunchId) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch can be cancelled on the lunches page");
    }

    @Override
    public void assertCancelled(String lunchId) {
        throw new UnsupportedOperationException(
                "Not implemented yet: Cancelled Lunches are listed on the lunches page");
    }

    @Override
    public void comment(String lunchId, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch takes comments on the lunches page");
    }

    @Override
    public void assertCommentRejectedAsBlank(String lunchId, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a blank comment is refused on the lunches page");
    }

    @Override
    public void assertComment(String lunchId, String author, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch shows its comments on the lunches page");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException("Not implemented yet: the Lunches need a login on the lunches page");
    }
}
