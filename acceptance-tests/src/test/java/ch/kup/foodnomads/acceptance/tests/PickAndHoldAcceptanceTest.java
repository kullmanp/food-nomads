package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class PickAndHoldAcceptanceTest extends AcceptanceTest {

    @Test
    void pickedVenueBecomesVisitedWhenTheLunchIsHeld() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: now", "date: today");
        lunches.addCandidate("lunch: now", "venue: Sternen");

        lunches.pick("lunch: now", "venue: Sternen");
        lunches.assertPlanned("lunch: now", "date: today", "venue: Sternen");

        lunches.hold("lunch: now");
        lunches.assertHeld("lunch: now", "date: today", "venue: Sternen");
        venues.assertVisited("name: Sternen");
    }

    @Test
    void lunchIsNotHeldBeforeItsDate() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.pick("lunch: friday", "venue: Sternen");

        lunches.hold("lunch: friday", "expect: notDue");
    }

    @Test
    void lunchIsNotHeldWithoutAVenue() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: now", "date: today");

        lunches.hold("lunch: now", "expect: noVenue");
    }

    @Test
    void pickedCandidateVenueCannotBeRemoved() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.pick("lunch: friday", "venue: Sternen");

        lunches.removeCandidate("lunch: friday", "venue: Sternen", "expect: candidatePicked");
    }

    @Test
    void cancelledLunchLeavesItsPickedVenueUnvisited() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: now", "date: today");
        lunches.addCandidate("lunch: now", "venue: Sternen");
        lunches.pick("lunch: now", "venue: Sternen");

        lunches.cancel("lunch: now");

        venues.assertNotVisited("name: Sternen");
    }
}
