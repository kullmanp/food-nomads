package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Planning and cancelling Lunches, and talking them over in their Lunch Comments. */
@ApplicationScoped
public class Lunches {

    /** The group lunches in Basel, so that's where "today" is. */
    private static final ZoneId BASEL = ZoneId.of("Europe/Zurich");

    /** The Planned Lunch, then the others latest first. */
    private static final Comparator<Lunch> LIST_ORDER = Comparator.comparing((Lunch l) -> !l.isPlanned())
            .thenComparing((Lunch l) -> l.date, Comparator.reverseOrder())
            .thenComparing((Lunch l) -> l.createdAt, Comparator.reverseOrder())
            .thenComparing(l -> l.id);

    private final LunchRepository repository;
    private final Clock clock;

    public Lunches(LunchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Today's date in Basel. */
    public LocalDate today() {
        return clock.instant().atZone(BASEL).toLocalDate();
    }

    /**
     * Plans a Lunch on this date. Fails with {@link DateInPastException} before today and with
     * {@link LunchAlreadyPlannedException} while another Lunch is planned.
     */
    @Transactional
    public Lunch plan(LocalDate date) {
        if (date.isBefore(today())) {
            throw new DateInPastException();
        }
        if (repository.planned().isPresent()) {
            throw new LunchAlreadyPlannedException();
        }
        Lunch lunch = new Lunch();
        lunch.id = UUID.randomUUID();
        lunch.date = date;
        lunch.state = LunchState.PLANNED;
        lunch.createdAt = clock.instant();
        repository.add(lunch);
        return lunch;
    }

    /**
     * Calls the Planned Lunch off. It stays, as a Cancelled Lunch. Fails with
     * {@link LunchNotPlannedException} if it is cancelled already.
     */
    @Transactional
    public void cancel(UUID id) {
        Lunch lunch = repository.findForUpdate(id).orElseThrow(LunchNotFoundException::new);
        if (!lunch.isPlanned()) {
            throw new LunchNotPlannedException();
        }
        lunch.state = LunchState.CANCELLED;
    }

    @Transactional
    public Optional<Lunch> planned() {
        return repository.planned();
    }

    /** The Lunch with its comments and their authors loaded, or {@link LunchNotFoundException}. */
    @Transactional
    public Lunch lunch(UUID id) {
        return repository.find(id).orElseThrow(LunchNotFoundException::new);
    }

    /** All Lunches: the Planned Lunch first, then the others latest first. */
    @Transactional
    public List<Lunch> all() {
        return repository.all().stream().sorted(LIST_ORDER).toList();
    }

    /**
     * Adds a Lunch Comment by this Member, stripped. Any Lunch takes comments, planned or not.
     * Fails with {@link CommentTextRequiredException} if there is no text.
     */
    @Transactional
    public LunchComment comment(UUID lunchId, Member author, String text) {
        Lunch lunch = repository.find(lunchId).orElseThrow(LunchNotFoundException::new);
        String stripped = text == null ? "" : text.strip();
        if (stripped.isEmpty()) {
            throw new CommentTextRequiredException();
        }
        LunchComment comment = new LunchComment();
        comment.id = UUID.randomUUID();
        comment.lunch = lunch;
        comment.author = author;
        comment.text = stripped;
        comment.writtenAt = clock.instant();
        lunch.comments.add(comment);
        repository.addComment(comment);
        return comment;
    }
}
