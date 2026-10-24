package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.util.regex.Pattern;

class UiVenueDriver implements VenueDriver {

    private final Page page;
    private final URI baseUrl;

    UiVenueDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        page.navigate(baseUrl.resolve("venues").toString());

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        assertThat(page.locator("main#login")).isVisible();
        assertThat(page.locator("main#venues")).not().isAttached();
    }
}
