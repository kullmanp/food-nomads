package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;

/** DSL for Members: joining, and later logging in and out. */
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
}
