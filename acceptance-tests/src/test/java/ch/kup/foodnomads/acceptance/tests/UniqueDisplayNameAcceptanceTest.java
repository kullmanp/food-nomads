package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class UniqueDisplayNameAcceptanceTest extends AcceptanceTest {

    @Test
    void displayNameMustBeUniqueAndARejectedNameLeavesTheInviteValid() {
        members.logIn("member: founder");
        invites.create("invite: first");
        invites.create("invite: second");
        members.logOut();
        members.join("invite: first", "displayName: Anna");
        members.logOut();

        members.join("invite: second", "displayName: Anna", "expect: nameTaken");

        members.join("invite: second", "displayName: Ben");
        members.assertLoggedIn("member: Ben");
    }

    @Test
    void displayNameIsRequired() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();

        members.join("invite: forAnna", "displayName: ", "expect: nameRequired");
    }
}
