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
 * A Member's up or down on a Candidate Venue. A Member has at most one per Candidate Venue. It
 * goes with the Candidate Venue when that is removed, and otherwise stays with the Lunch.
 */
@Entity
public class Vote extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_venue_id")
    public CandidateVenue candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    public Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public VoteDirection direction;
}
