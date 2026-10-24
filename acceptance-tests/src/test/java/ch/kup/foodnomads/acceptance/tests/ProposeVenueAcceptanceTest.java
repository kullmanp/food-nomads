package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class ProposeVenueAcceptanceTest extends AcceptanceTest {

    @Test
    void proposedVenueAppearsAmongTheCandidates() {
        members.logIn("member: founder");

        venues.propose("name: Sternen", "address: Spalenberg 1, Basel");

        venues.assertCandidate("name: Sternen", "address: Spalenberg 1, Basel", "proposedBy: founder");
    }

    @Test
    void venueNeedsAName() {
        members.logIn("member: founder");

        venues.propose("name: ", "expect: rejected");
    }
}
