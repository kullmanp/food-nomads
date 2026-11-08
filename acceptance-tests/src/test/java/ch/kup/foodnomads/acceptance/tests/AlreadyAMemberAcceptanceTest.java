package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class AlreadyAMemberAcceptanceTest extends AcceptanceTest {

    @Test
    void memberCannotJoinAgainAndTheInviteStaysValid() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");

        members.join("invite: forAnna", "displayName: Anna", "expect: alreadyMember");

        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.assertLoggedIn("member: Anna");
    }
}
