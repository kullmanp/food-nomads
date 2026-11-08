package ch.kup.foodnomads.acceptance.drivers.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.Passkey;
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
        // Whoever logged in on this browser before isn't the person joining.
        authenticator.removePasskeys();
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
    public void logIn(String displayName) {
        Passkey passkey = Passkeys.of(displayName);
        authenticator.holdPasskey(passkey);

        page.navigate(baseUrl.resolve("login").toString());
        page.locator("#log-in-button").click();

        assertThat(page.locator("#current-member")).hasText(displayName);
        // The authenticator counted this login, and the app expects the count to keep rising.
        Passkeys.store(displayName, authenticator.passkey(passkey.credentialId()));
    }

    @Override
    public void logOut() {
        page.navigate(baseUrl.toString());
        page.locator("#log-out-button").click();

        assertThat(page.locator("#log-in-link")).isVisible();
    }

    @Override
    public void assertLoggedOut() {
        page.navigate(baseUrl.toString());

        assertThat(page.locator("#log-in-link")).isVisible();
        assertThat(page.locator("#current-member")).not().isAttached();
    }

    @Override
    public void assertInviteRejected(URI joinUrl, String displayName) {
        page.navigate(joinUrl.toString());

        assertThat(page.locator("main#join-rejected")).isVisible();
        assertThat(page.locator("#join-form")).not().isAttached();
    }

    @Override
    public void assertDisplayNameTaken(URI joinUrl, String displayName) {
        throw new UnsupportedOperationException(
                "Not implemented yet: the join form shows the name is taken, with no passkey prompt");
    }

    @Override
    public void assertDisplayNameRequired(URI joinUrl, String displayName) {
        throw new UnsupportedOperationException(
                "Not implemented yet: the join form asks for a name, with no passkey prompt");
    }
}
