package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class VotesAcceptanceTest extends AcceptanceTest {

    @Test
    void votesAreCountedPerCandidateVenue() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        venues.propose("name: Sternen");
        venues.propose("name: Kunsthalle");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Kunsthalle");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");
        lunches.vote("lunch: friday", "venue: Kunsthalle", "direction: down");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 2", "down: 0");
        lunches.assertVotes("lunch: friday", "venue: Kunsthalle", "up: 0", "down: 1");
    }

    @Test
    void memberChangesAndWithdrawsTheirVote() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.vote("lunch: friday", "venue: Sternen", "direction: down");
        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 1");

        lunches.withdrawVote("lunch: friday", "venue: Sternen");
        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 0");
    }

    @Test
    void removedCandidateVenueLosesItsVotes() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.removeCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Sternen");

        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 0");
    }
}
