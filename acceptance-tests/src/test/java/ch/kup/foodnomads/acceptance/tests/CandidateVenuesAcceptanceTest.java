package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class CandidateVenuesAcceptanceTest extends AcceptanceTest {

    @Test
    void memberPutsUpAndRemovesCandidateVenues() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        venues.propose("name: Kunsthalle");
        lunches.plan("lunch: friday", "date: +7");

        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Kunsthalle");
        lunches.removeCandidate("lunch: friday", "venue: Kunsthalle");

        lunches.assertCandidates("lunch: friday", "venues: Sternen");
    }

    @Test
    void visitedVenueCannotBeACandidate() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");
        lunches.plan("lunch: friday", "date: +7");

        lunches.addCandidate("lunch: friday", "venue: Sternen", "expect: venueVisited");
    }

    @Test
    void candidateVenueThatBecomesVisitedStaysListedAsVisited() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");

        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.assertCandidates("lunch: friday", "venues: Sternen", "visited: Sternen");
    }
}
