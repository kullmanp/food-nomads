package ch.kup.foodnomads.lunches;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** One meetup of the group on a date. There is at most one Planned Lunch at a time. */
@Entity
public class Lunch extends PanacheEntityBase {

    private static final Comparator<LunchComment> OLDEST_FIRST =
            Comparator.comparing((LunchComment c) -> c.writtenAt).thenComparing(c -> c.id);

    @Id
    public UUID id;

    /** The day only. The time is arranged in the chat. */
    @Column(nullable = false)
    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LunchState state;

    /** When it was planned. Orders Lunches on the same date. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @OneToMany(mappedBy = "lunch")
    public List<LunchComment> comments = new ArrayList<>();

    public boolean isPlanned() {
        return state == LunchState.PLANNED;
    }

    /** The Lunch Comments as a chat, oldest first. */
    public List<LunchComment> chat() {
        return comments.stream().sorted(OLDEST_FIRST).toList();
    }
}
