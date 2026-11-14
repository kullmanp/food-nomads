package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class RecordPastLunchAcceptanceTest extends AcceptanceTest {

    @Test
    void recordedLunchMakesItsVenueVisited() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");

        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.assertHeld("lunch: spring", "date: -60", "venue: Sternen");
        venues.assertVisited("name: Sternen");
    }

    @Test
    void visitedVenueCannotBeRecordedAgain() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");
        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.recordPast("lunch: summer", "date: -30", "venue: Sternen", "expect: venueVisited");
    }

    @Test
    void recordedLunchMustLieInThePast() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");

        lunches.recordPast("lunch: now", "date: today", "venue: Sternen", "expect: dateNotPast");
        venues.assertNotVisited("name: Sternen");
    }
}
