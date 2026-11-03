package ch.kup.foodnomads.venues;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A {@link VenueRepository} for unit tests. */
class InMemoryVenueRepository implements VenueRepository {

    private final List<Venue> venues = new ArrayList<>();

    @Override
    public void add(Venue venue) {
        venues.add(venue);
    }

    @Override
    public List<Venue> newestFirst() {
        return venues.stream()
                .sorted(Comparator.comparing((Venue v) -> v.proposedAt).reversed().thenComparing(v -> v.id))
                .toList();
    }
}
