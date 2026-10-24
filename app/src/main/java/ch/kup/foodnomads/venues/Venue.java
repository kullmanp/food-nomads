package ch.kup.foodnomads.venues;

import ch.kup.foodnomads.members.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;

/** A place the group could go for lunch. */
@Entity
public class Venue extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String name;

    /** Null if the proposing Member didn't give one. */
    public String address;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_by")
    public Member proposedBy;

    @Column(name = "proposed_at", nullable = false)
    public Instant proposedAt;
}
