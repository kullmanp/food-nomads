package ch.kup.foodnomads.members;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** A single-use, time-limited link one Member gives to a new person so they can join. */
@Entity
public class Invite extends PanacheEntityBase {

    private static final Duration VALID_FOR = Duration.ofDays(7);

    @Id
    public UUID id;

    /** Stored in plain text, because the link has to stay available. */
    @Column(nullable = false)
    public String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    public Member createdBy;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    /** Null until redeemed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_by")
    public Member redeemedBy;

    /** Null until redeemed. */
    @Column(name = "redeemed_at")
    public Instant redeemedAt;

    public Instant expiresAt() {
        return createdAt.plus(VALID_FOR);
    }

    boolean isOpenAt(Instant now) {
        return redeemedAt == null && now.isBefore(expiresAt());
    }
}
