package ch.kup.foodnomads.venues;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

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
}
