package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import com.microsoft.playwright.Page;
import java.net.URI;

class UiInviteDriver implements InviteDriver {

    private final Page page;
    private final URI baseUrl;

    UiInviteDriver(Page page, URI baseUrl) {
        this.page = page;
        this.baseUrl = baseUrl;
    }

    @Override
    public URI create() {
        throw new UnsupportedOperationException("Not implemented yet: /invites creates an Invite and shows its link");
    }

    @Override
    public void assertNotAccessibleWithoutLogin() {
        throw new UnsupportedOperationException("Not implemented yet: /invites redirects to login without a Member");
    }
}
