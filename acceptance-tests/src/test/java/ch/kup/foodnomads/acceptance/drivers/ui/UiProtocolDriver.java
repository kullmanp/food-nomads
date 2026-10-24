package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.SystemDriver;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URI;

/**
 * Talks to the system through the browser, like a Member on their phone. The browser is
 * started once per run; each test gets a fresh context.
 */
public class UiProtocolDriver implements ProtocolDriver {

    private final BrowserContext context;
    private final Page page;
    private final URI baseUrl;
    private final VirtualAuthenticator authenticator;

    public UiProtocolDriver(URI baseUrl) {
        this.baseUrl = baseUrl;
        this.context = SharedBrowser.INSTANCE.newContext(phone());
        this.page = context.newPage();
        this.authenticator = new VirtualAuthenticator(context.newCDPSession(page));
    }

    @Override
    public SystemDriver system() {
        return new UiSystemDriver(page, baseUrl);
    }

    @Override
    public MemberDriver members() {
        return new UiMemberDriver(page, baseUrl, authenticator);
    }

    @Override
    public void close() {
        context.close();
    }

    private static Browser.NewContextOptions phone() {
        return new Browser.NewContextOptions()
                .setViewportSize(390, 844)
                .setDeviceScaleFactor(3)
                .setIsMobile(true)
                .setHasTouch(true);
    }

    private static final class SharedBrowser {
        static final Browser INSTANCE = launch();

        private static Browser launch() {
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch();
            Runtime.getRuntime().addShutdownHook(new Thread(playwright::close));
            return browser;
        }
    }
}
