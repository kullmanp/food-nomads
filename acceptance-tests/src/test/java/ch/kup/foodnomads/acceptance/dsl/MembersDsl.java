package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;
import java.net.URI;

/**
 * DSL for Members: joining, logging in and logging out. Member names are aliased for functional
 * isolation, except {@code founder}, who is shared by all tests and joins under that exact name.
 */
public class MembersDsl {

    private final MemberDriver driver;
    private final InviteLinks inviteLinks;
    private final Aliases aliases = new Aliases();

    MembersDsl(MemberDriver driver, InviteLinks inviteLinks) {
        this.driver = driver;
        this.inviteLinks = inviteLinks;
    }

    /**
     * Joins through the Invite with this alias. With {@code expect: rejected}, fails unless the
     * Invite is refused. With {@code expect: nameTaken} or {@code expect: nameRequired}, fails
     * unless the display name is refused and no passkey was registered.
     */
    public void join(String... args) {
        Params params = Params.parse(
                args, Params.required("invite"), Params.required("displayName"), Params.optional("expect", "success"));
        URI link = inviteLinks.of(params.value("invite"));
        String displayName = realName(params.value("displayName"));
        switch (params.value("expect")) {
            case "success" -> driver.join(link, displayName);
            case "rejected" -> driver.assertInviteRejected(link, displayName);
            case "nameTaken" -> driver.assertDisplayNameTaken(link, displayName);
            case "nameRequired" -> driver.assertDisplayNameRequired(link, displayName);
            default ->
                throw new IllegalArgumentException("Unknown expect '" + params.value("expect")
                        + "', expected success, rejected, nameTaken or nameRequired");
        }
    }

    /**
     * Fails unless redeeming the Bootstrap Invite is refused. By the time any test runs, the
     * harness has already redeemed it as {@code founder}, so a Member always exists.
     */
    public void assertBootstrapInviteRejected() {
        driver.assertInviteRejected(SystemUnderTest.get().bootstrapInviteUrl(), "Intruder");
    }

    /** Logs in as {@code member} with the passkey they registered when joining. */
    public void logIn(String... args) {
        Params params = Params.parse(args, Params.required("member"));
        driver.logIn(realName(params.value("member")));
    }

    public void logOut() {
        driver.logOut();
    }

    public void assertLoggedIn(String... args) {
        Params params = Params.parse(args, Params.required("member"));
        driver.assertLoggedIn(realName(params.value("member")));
    }

    public void assertLoggedOut() {
        driver.assertLoggedOut();
    }

    private String realName(String alias) {
        return Founder.ALIAS.equals(alias) ? alias : aliases.realName(alias);
    }
}
