package ch.kup.foodnomads.venues;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Keeps Venues in PostgreSQL. Callers run it inside a transaction. */
@ApplicationScoped
class PanacheVenueRepository implements VenueRepository {

    @Override
    public void add(Venue venue) {
        venue.proposedBy = Member.getEntityManager().merge(venue.proposedBy);
        venue.persist();
    }

    @Override
    public List<Venue> newestFirst() {
        return Venue.find("from Venue v join fetch v.proposedBy order by v.proposedAt desc, v.id")
                .list();
    }

    @Override
    public Optional<Venue> findForUpdate(UUID id) {
        return Venue.find("id", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResultOptional();
    }
}
