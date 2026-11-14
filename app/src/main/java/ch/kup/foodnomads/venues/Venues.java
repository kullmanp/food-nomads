package ch.kup.foodnomads.venues;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proposing Venues and listing the candidates. */
@ApplicationScoped
public class Venues {

    private final VenueRepository repository;

    public Venues(VenueRepository repository) {
        this.repository = repository;
    }

    /**
     * Proposes a Venue on behalf of this Member. Fails with {@link IllegalArgumentException} if
     * the name is missing. A blank address counts as none.
     */
    @Transactional
    public Venue propose(Member proposer, String name, String address) {
        String trimmedName = name == null ? "" : name.strip();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("A Venue needs a name");
        }
        String trimmedAddress = address == null ? "" : address.strip();

        Venue venue = new Venue();
        venue.id = UUID.randomUUID();
        venue.name = trimmedName;
        venue.address = trimmedAddress.isEmpty() ? null : trimmedAddress;
        venue.proposedBy = proposer;
        venue.proposedAt = Instant.now();
        repository.add(venue);
        return venue;
    }

    /** All Venues, newest first, with their proposing Member loaded. */
    @Transactional
    public List<Venue> candidates() {
        return repository.newestFirst();
    }

    /**
     * The Venue, held for the caller's transaction, so that two Lunches can't make it visited at
     * once. Fails with {@link VenueNotFoundException} if there is none with this id.
     */
    @Transactional
    public Venue lock(UUID id) {
        return repository.findForUpdate(id).orElseThrow(VenueNotFoundException::new);
    }
}
