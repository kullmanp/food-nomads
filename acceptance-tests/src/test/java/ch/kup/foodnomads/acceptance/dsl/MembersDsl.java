package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;

/**
 * DSL for Members: joining, logging in and logging out. Member names aren't aliased: the only
 * Member so far is {@code founder}, who is shared by all tests and joins under that exact name.
 */
public class MembersDsl {

    private final MemberDriver driver;

    MembersDsl(MemberDriver driver) {
        this.driver = driver;
    }

    /**
     * Fails unless redeeming the Bootstrap Invite is refused. By the time any test runs, the
     * harness has already redeemed it as {@code founder}, so a Member always exists.
     */
    public void assertBootstrapInviteRejected() {
        driver.assertBootstrapInviteRejected(SystemUnderTest.get().bootstrapInviteUrl());
    }

    /** Logs in as {@code member} with the passkey they registered when joining. */
    public void logIn(String... args) {
        Params params = Params.parse(args, Params.required("member"));
        driver.logIn(params.value("member"));
    }

    public void logOut() {
        driver.logOut();
    }

    public void assertLoggedIn(String... args) {
        Params params = Params.parse(args, Params.required("member"));
        driver.assertLoggedIn(params.value("member"));
    }

    public void assertLoggedOut() {
        driver.assertLoggedOut();
    }
}
