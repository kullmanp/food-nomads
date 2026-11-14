package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.venues.Venue;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.text.Collator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** One meetup of the group on a date. There is at most one Planned Lunch at a time. */
@Entity
public class Lunch extends PanacheEntityBase {

    private static final Comparator<LunchComment> OLDEST_FIRST =
            Comparator.comparing((LunchComment c) -> c.writtenAt).thenComparing(c -> c.id);

    /** By display name as a German speaker sorts it. Display names are unique, so that's enough. */
    private static final Comparator<Member> BY_NAME =
            Comparator.comparing((Member m) -> m.displayName, Collator.getInstance(Locale.GERMAN));

    @Id
    public UUID id;

    /** The day only. The time is arranged in the chat. */
    @Column(nullable = false)
    public LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public LunchState state;

    /** Where it took place. Null until a Venue is picked. A Held Lunch always has one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    public Venue venue;

    /** When it was planned or recorded. Orders Lunches on the same date. */
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @OneToMany(mappedBy = "lunch")
    public List<LunchComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "lunch")
    public List<CandidateVenue> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "lunch")
    public List<Rsvp> rsvps = new ArrayList<>();

    public boolean isPlanned() {
        return state == LunchState.PLANNED;
    }

    public boolean isHeld() {
        return state == LunchState.HELD;
    }

    /** The Lunch Comments as a chat, oldest first. */
    public List<LunchComment> chat() {
        return comments.stream().sorted(OLDEST_FIRST).toList();
    }

    /** The Members who answered {@code answer}, by display name. */
    public List<Member> answered(RsvpAnswer answer) {
        return rsvps.stream()
                .filter(rsvp -> rsvp.answer == answer)
                .map(rsvp -> rsvp.member)
                .sorted(BY_NAME)
                .toList();
    }

    /** How many Members are in. Doesn't need the Members themselves. */
    public int inCount() {
        return (int) rsvps.stream().filter(rsvp -> rsvp.answer == RsvpAnswer.IN).count();
    }

    /** The Member's answer, or null if they haven't answered. Found by the Member's id. */
    public RsvpAnswer answerOf(Member member) {
        return rsvpOf(member).map(rsvp -> rsvp.answer).orElse(null);
    }

    Optional<Rsvp> rsvpOf(Member member) {
        return rsvps.stream().filter(rsvp -> rsvp.member.id.equals(member.id)).findFirst();
    }
}
