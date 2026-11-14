package ch.kup.foodnomads.venues;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A {@link VenueRepository} for unit tests, also of the packages that build on Venues. */
public class InMemoryVenueRepository implements VenueRepository {

    private final List<Venue> venues = new ArrayList<>();
    private final List<UUID> locked = new ArrayList<>();

    @Override
    public void add(Venue venue) {
        venues.add(venue);
    }

    @Override
    public List<Venue> newestFirst() {
        return venues.stream()
                .sorted(Comparator.comparing((Venue v) -> v.proposedAt)
                        .reversed()
                        .thenComparing(v -> v.id))
                .toList();
    }

    @Override
    public Optional<Venue> find(UUID id) {
        return venues.stream().filter(venue -> venue.id.equals(id)).findFirst();
    }

    @Override
    public Optional<Venue> findForUpdate(UUID id) {
        locked.add(id);
        return find(id);
    }

    /** The ids {@link #findForUpdate} was asked for, in order. */
    public List<UUID> locked() {
        return List.copyOf(locked);
    }
}
