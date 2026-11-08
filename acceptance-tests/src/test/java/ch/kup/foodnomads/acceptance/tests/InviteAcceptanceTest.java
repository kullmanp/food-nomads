package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class InviteAcceptanceTest extends AcceptanceTest {

    @Test
    void invitedPersonJoinsAndIsLoggedIn() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();

        members.join("invite: forAnna", "displayName: Anna");

        members.assertLoggedIn("member: Anna");
    }

    @Test
    void newMemberCanLogBackInWithTheirPasskey() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.logOut();

        members.logIn("member: Anna");

        members.assertLoggedIn("member: Anna");
    }

    @Test
    void inviteCanBeRedeemedOnlyOnce() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.logOut();

        members.join("invite: forAnna", "displayName: Ben", "expect: rejected");
    }

    @Test
    void visitorWhoIsNotLoggedInCannotCreateInvites() {
        invites.assertNotAccessibleWithoutLogin();
    }
}
