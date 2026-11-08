package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.util.regex.Pattern;

class UiInviteDriver implements InviteDriver {

    private final Page page;
    private final URI baseUrl;

    UiInviteDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public URI create() {
        page.navigate(baseUrl.toString());
        page.locator("#invites-link").click();
        page.locator("#create-invite-button").click();

        assertThat(page.locator("#invite-link")).isVisible();
        page.locator("#copy-invite").click();
        assertThat(page.locator("#invite-copied")).isVisible();
        return URI.create(page.locator("#invite-link").inputValue());
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        page.navigate(baseUrl.resolve("invites").toString());

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        assertThat(page.locator("main#login")).isVisible();
        assertThat(page.locator("main#invites")).not().isAttached();
    }
}
