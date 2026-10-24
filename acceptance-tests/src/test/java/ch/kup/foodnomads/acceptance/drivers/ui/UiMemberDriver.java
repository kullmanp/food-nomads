package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.Passkeys;
import com.microsoft.playwright.Page;
import java.net.URI;

class UiMemberDriver implements MemberDriver {

    private final Page page;
    private final URI baseUrl;
    private final VirtualAuthenticator authenticator;

    UiMemberDriver(Page page, URI baseUrl, VirtualAuthenticator authenticator) {
        this.page = page;
        this.baseUrl = baseUrl;
        this.authenticator = authenticator;
    }

    @Override
    public void join(URI joinUrl, String displayName) {
        page.navigate(joinUrl.toString());
        page.locator("#display-name").fill(displayName);
        page.locator("#join-button").click();

        assertThat(page.locator("#current-member")).hasText(displayName);
        Passkeys.store(displayName, authenticator.onlyPasskey());
    }

    @Override
    public void assertLoggedIn(String displayName) {
        page.navigate(baseUrl.toString());

        assertThat(page.locator("#current-member")).hasText(displayName);
    }

    @Override
    public void assertBootstrapInviteRejected(URI joinUrl) {
        page.navigate(joinUrl.toString());

        assertThat(page.locator("main#join-rejected")).isVisible();
        assertThat(page.locator("#join-form")).not().isAttached();
    }
}
