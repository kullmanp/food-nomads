package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

/**
 * A Member's answer, in or out, about attending a Planned Lunch. A Member has at most one per
 * Lunch. Moving the Lunch removes them all; otherwise they stay with the Lunch.
 */
@Entity
public class Rsvp extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lunch_id")
    public Lunch lunch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    public Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RsvpAnswer answer;
}
