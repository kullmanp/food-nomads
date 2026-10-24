package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import com.microsoft.playwright.Page;
import java.net.URI;

class UiVenueDriver implements VenueDriver {

    private final Page page;
    private final URI baseUrl;

    UiVenueDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException(
                "Not implemented yet: the Venue list page redirects to login without a logged-in Member");
    }
}
