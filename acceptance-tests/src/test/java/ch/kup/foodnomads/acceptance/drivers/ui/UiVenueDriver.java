package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.VenueDriver;
import com.microsoft.playwright.Locator;
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
    public void propose(String name, String address) {
        submitProposal(name, address);

        assertThat(page.locator("#propose-error")).not().isAttached();
        assertThat(candidate(name)).isVisible();
    }

    @Override
    public void assertProposalRejected(String name, String address) {
        submitProposal(name, address);

        assertThat(page.locator("#propose-error")).isVisible();
    }

    @Override
    public void assertCandidate(String name, String address, String proposedBy) {
        page.navigate(baseUrl.resolve("venues").toString());

        Locator candidate = candidate(name);
        assertThat(candidate).hasCount(1);
        if (address.isEmpty()) {
            assertThat(candidate.locator(".venue-address")).not().isAttached();
        } else {
            assertThat(candidate.locator(".venue-address")).hasText(address);
        }
        assertThat(candidate.locator(".venue-proposed-by")).hasText(proposedBy);
    }

    private void submitProposal(String name, String address) {
        page.navigate(baseUrl.resolve("venues").toString());
        page.locator("#venue-name").fill(name);
        page.locator("#venue-address").fill(address);
        page.waitForResponse(r -> r.url().endsWith("/venues") && "POST".equals(r.request().method()),
                () -> page.locator("#propose-button").click());
    }

    private Locator candidate(String name) {
        return page.locator("#candidates .candidate")
                .filter(new Locator.FilterOptions().setHas(page.locator(".venue-name")
                        .getByText(name, new Locator.GetByTextOptions().setExact(true))));
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        page.navigate(baseUrl.resolve("venues").toString());

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        assertThat(page.locator("main#login")).isVisible();
        assertThat(page.locator("main#venues")).not().isAttached();
    }
}
