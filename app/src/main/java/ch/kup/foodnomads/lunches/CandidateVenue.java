package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.venues.Venue;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A Venue a Member put up for a Planned Lunch. It belongs to the Lunch and stays with it, also
 * once the Lunch is held or cancelled.
 */
@Entity
@Table(name = "candidate_venue")
public class CandidateVenue extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lunch_id")
    public Lunch lunch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id")
    public Venue venue;
}
