package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A Member's remark on a Lunch, before it or after it. It can't be edited or deleted. */
@Entity
@Table(name = "lunch_comment")
public class LunchComment extends PanacheEntityBase {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lunch_id")
    public Lunch lunch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    public Member author;

    @Column(nullable = false)
    public String text;

    @Column(name = "written_at", nullable = false)
    public Instant writtenAt;
}
