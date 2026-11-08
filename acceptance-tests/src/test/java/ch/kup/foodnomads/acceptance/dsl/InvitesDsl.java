package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;

/** DSL for Invites. An Invite alias stands for the link the driver got back when creating it. */
public class InvitesDsl {

    private final InviteDriver driver;
    private final InviteLinks links;

    InvitesDsl(InviteDriver driver, InviteLinks links) {
        this.driver = driver;
        this.links = links;
    }

    /** Creates an Invite as the logged-in Member and remembers its link under {@code invite}. */
    public void create(String... args) {
        Params params = Params.parse(args, Params.required("invite"));
        links.remember(params.value("invite"), SystemUnderTest.get().pointedAtApp(driver.create()));
    }

    /** Fails unless creating an Invite is refused to someone who isn't logged in. */
    public void assertNotAccessibleWithoutLogin() {
        driver.assertNotAccessibleWithoutLogin();
    }
}
