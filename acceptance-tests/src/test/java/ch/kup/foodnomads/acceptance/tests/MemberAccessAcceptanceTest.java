package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class MemberAccessAcceptanceTest extends AcceptanceTest {

    @Test
    void memberCanLogOutAndLogBackInWithTheirPasskey() {
        members.logIn("member: founder");
        members.logOut();
        members.assertLoggedOut();

        members.logIn("member: founder");
        members.assertLoggedIn("member: founder");
    }

    @Test
    void visitorWhoIsNotLoggedInCannotSeeVenues() {
        venues.assertNotAccessibleWithoutLogin();
    }
}
