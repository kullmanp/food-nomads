package ch.kup.foodnomads.venues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ch.kup.foodnomads.members.Member;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VenuesTest {

    private final InMemoryVenueRepository repository = new InMemoryVenueRepository();
    private final Venues venues = new Venues(repository);
    private final Member anna = member("Anna");

    @Test
    void stripsTheName() {
        Venue venue = venues.propose(anna, "  Sternen \t", null);

        assertThat(venue.name).isEqualTo("Sternen");
    }

    @Test
    void rejectsAMissingName() {
        assertThatIllegalArgumentException().isThrownBy(() -> venues.propose(anna, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> venues.propose(anna, " \t ", null));
        assertThat(repository.newestFirst()).isEmpty();
    }

    @Test
    void stripsTheAddress() {
        Venue venue = venues.propose(anna, "Sternen", " Hauptstrasse 1 ");

        assertThat(venue.address).isEqualTo("Hauptstrasse 1");
    }

    @Test
    void treatsABlankAddressAsNone() {
        assertThat(venues.propose(anna, "Sternen", "   ").address).isNull();
        assertThat(venues.propose(anna, "Rössli", null).address).isNull();
    }

    @Test
    void recordsWhoProposedItAndWhen() {
        Venue venue = venues.propose(anna, "Sternen", null);

        assertThat(venue.id).isNotNull();
        assertThat(venue.proposedBy).isSameAs(anna);
        assertThat(venue.proposedAt).isNotNull();
    }

    @Test
    void listsTheProposedVenue() {
        Venue venue = venues.propose(anna, "Sternen", null);

        assertThat(venues.newestFirst()).containsExactly(venue);
    }

    @Test
    void locksAVenueByItsId() {
        venues.propose(anna, "Rössli", null);
        Venue sternen = venues.propose(anna, "Sternen", null);

        assertThat(venues.lock(sternen.id)).isSameAs(sternen);
        assertThat(repository.locked()).containsExactly(sternen.id);
    }

    @Test
    void findsAVenueByItsIdWithoutLockingIt() {
        venues.propose(anna, "Rössli", null);
        Venue sternen = venues.propose(anna, "Sternen", null);

        assertThat(venues.venue(sternen.id)).isSameAs(sternen);
        assertThat(repository.locked()).isEmpty();
    }

    @Test
    void anUnknownVenueIsNotFound() {
        venues.propose(anna, "Sternen", null);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(VenueNotFoundException.class).isThrownBy(() -> venues.venue(unknown));
    }

    @Test
    void anUnknownVenueCannotBeLocked() {
        venues.propose(anna, "Sternen", null);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(VenueNotFoundException.class).isThrownBy(() -> venues.lock(unknown));
    }

    private static Member member(String displayName) {
        Member member = new Member();
        member.id = UUID.randomUUID();
        member.displayName = displayName;
        return member;
    }
}
