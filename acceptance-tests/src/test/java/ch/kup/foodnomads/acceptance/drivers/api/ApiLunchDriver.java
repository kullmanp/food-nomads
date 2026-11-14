package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.LunchDriver;
import java.time.LocalDate;

class ApiLunchDriver implements LunchDriver {

    private final ApiClient api;

    ApiLunchDriver(ApiClient api) {
        this.api = api;
    }

    @Override
    public String plan(LocalDate date) {
        throw new UnsupportedOperationException("Not implemented yet: plans a Lunch on /api/lunches");
    }

    @Override
    public void assertPlanRejectedAsInThePast(LocalDate date) {
        throw new UnsupportedOperationException(
                "Not implemented yet: planning before today is refused on /api/lunches");
    }

    @Override
    public void assertPlanRejectedAsAlreadyPlanned(LocalDate date) {
        throw new UnsupportedOperationException(
                "Not implemented yet: only one Lunch is planned at a time on /api/lunches");
    }

    @Override
    public void cancelPlannedLunch() {
        throw new UnsupportedOperationException(
                "Not implemented yet: the Planned Lunch can be cancelled on /api/lunches");
    }

    @Override
    public void assertPlanned(String lunchId, LocalDate date, String venue) {
        throw new UnsupportedOperationException("Not implemented yet: the Planned Lunch is shown on /api/lunches");
    }

    @Override
    public void cancel(String lunchId) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch can be cancelled on /api/lunches");
    }

    @Override
    public void assertCancelled(String lunchId) {
        throw new UnsupportedOperationException("Not implemented yet: Cancelled Lunches are listed on /api/lunches");
    }

    @Override
    public void comment(String lunchId, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch takes comments on /api/lunches");
    }

    @Override
    public void assertCommentRejectedAsBlank(String lunchId, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a blank comment is refused on /api/lunches");
    }

    @Override
    public void assertComment(String lunchId, String author, String text) {
        throw new UnsupportedOperationException("Not implemented yet: a Lunch shows its comments on /api/lunches");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException("Not implemented yet: the Lunches need a login on /api/lunches");
    }
}
