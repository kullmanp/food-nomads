package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import java.net.URI;

class UiSystemDriver implements SystemDriver {

    private final Page page;
    private final URI baseUrl;

    UiSystemDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public void assertAvailable() {
        Response response = page.navigate(baseUrl.toString());

        assertNotNull(response, "No response for the home page");
        assertTrue(response.ok(), "Home page answered with " + response.status());
        assertThat(page.locator("main#home")).isVisible();
    }
}
