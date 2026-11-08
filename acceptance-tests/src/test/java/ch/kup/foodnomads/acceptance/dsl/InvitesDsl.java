package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.InviteDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;

/** DSL for Invites. An Invite alias stands for the link the driver got back when creating it. */
public class InvitesDsl {

    private final InviteDriver driver;
    private final InviteLinks links;
    private final MembersDsl members;

    InvitesDsl(InviteDriver driver, InviteLinks links, MembersDsl members) {
        this.driver = driver;
        this.links = links;
        this.members = members;
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

    /** Fails unless the Invite {@code invite} is listed as open, created by {@code createdBy}. */
    public void assertOpen(String... args) {
        Params params = Params.parse(args, Params.required("invite"), Params.required("createdBy"));
        driver.assertOpen(links.of(params.value("invite")), members.realName(params.value("createdBy")));
    }

    /** Fails unless the Invite {@code invite} is missing from the list of Open Invites. */
    public void assertNotOpen(String... args) {
        Params params = Params.parse(args, Params.required("invite"));
        driver.assertNotOpen(links.of(params.value("invite")));
    }

    /** Revokes the Invite {@code invite} as the logged-in Member. */
    public void revoke(String... args) {
        Params params = Params.parse(args, Params.required("invite"));
        driver.revoke(links.of(params.value("invite")));
    }
}
