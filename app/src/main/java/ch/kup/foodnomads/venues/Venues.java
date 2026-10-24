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
        venue.proposedBy = Member.getEntityManager().merge(proposer);
        venue.proposedAt = Instant.now();
        venue.persist();
        return venue;
    }

    /** All Venues, newest first, with their proposing Member loaded. */
    @Transactional
    public List<Venue> candidates() {
        return Venue.find("from Venue v join fetch v.proposedBy order by v.proposedAt desc, v.id").list();
    }
}
