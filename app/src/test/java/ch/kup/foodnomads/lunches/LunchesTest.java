package ch.kup.foodnomads.lunches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LunchesTest {

    /** 11:00 in Basel on 1 March 2026. */
    private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

    private static final LocalDate TODAY = LocalDate.parse("2026-03-01");

    private final MutableClock clock = new MutableClock(NOW);
    private final InMemoryLunchRepository repository = new InMemoryLunchRepository();
    private final Lunches lunches = new Lunches(repository, clock);

    private final Member anna = member("Anna");
    private final Member ben = member("Ben");

    @Test
    void planningStoresAPlannedLunchOnThatDate() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        assertThat(lunch.id).isNotNull();
        assertThat(lunch.date).isEqualTo(TODAY.plusDays(7));
        assertThat(lunch.state).isEqualTo(LunchState.PLANNED);
        assertThat(lunch.isPlanned()).isTrue();
        assertThat(lunch.createdAt).isEqualTo(NOW);
        assertThat(lunch.comments).isEmpty();
        assertThat(repository.all()).containsExactly(lunch);
    }

    @Test
    void everyLunchGetsItsOwnId() {
        Lunch first = lunches.plan(TODAY);
        lunches.cancel(first.id);

        assertThat(lunches.plan(TODAY).id).isNotEqualTo(first.id);
    }

    @Test
    void aLunchCanBePlannedForToday() {
        assertThat(lunches.plan(TODAY).date).isEqualTo(TODAY);
    }

    @Test
    void aLunchCannotBePlannedBeforeToday() {
        assertThatExceptionOfType(DateInPastException.class).isThrownBy(() -> lunches.plan(TODAY.minusDays(1)));
        assertThat(repository.all()).isEmpty();
    }

    @Test
    void justBeforeMidnightInBaselTodayIsStillThatDay() {
        // 23:59:59.999999999 in Basel (UTC+1 in winter) on 1 March.
        clock.advance(Duration.ofHours(13).minusNanos(1));

        assertThat(lunches.today()).isEqualTo(TODAY);
        assertThat(lunches.plan(TODAY).date).isEqualTo(TODAY);
    }

    @Test
    void atMidnightInBaselTheDayBeforeIsInThePast() {
        // 00:00 in Basel on 2 March, while it is still 1 March in UTC.
        clock.advance(Duration.ofHours(13));

        assertThat(lunches.today()).isEqualTo(TODAY.plusDays(1));
        assertThatExceptionOfType(DateInPastException.class).isThrownBy(() -> lunches.plan(TODAY));
        assertThat(lunches.plan(TODAY.plusDays(1)).date).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void inSummerMidnightInBaselIsTwoHoursBeforeUtc() {
        MutableClock summer = new MutableClock(Instant.parse("2026-07-01T21:59:59Z"));
        Lunches inSummer = new Lunches(repository, summer);
        assertThat(inSummer.today()).isEqualTo(LocalDate.parse("2026-07-01"));

        summer.advance(Duration.ofSeconds(1));

        assertThat(inSummer.today()).isEqualTo(LocalDate.parse("2026-07-02"));
    }

    @Test
    void onlyOneLunchIsPlannedAtATime() {
        Lunch planned = lunches.plan(TODAY.plusDays(7));

        assertThatExceptionOfType(LunchAlreadyPlannedException.class)
                .isThrownBy(() -> lunches.plan(TODAY.plusDays(14)));
        assertThat(repository.all()).containsExactly(planned);
    }

    @Test
    void thereIsNoPlannedLunchAtFirst() {
        assertThat(lunches.planned()).isEmpty();
    }

    @Test
    void thePlannedLunchIsTheOneThatWasPlanned() {
        Lunch cancelled = lunches.plan(TODAY);
        lunches.cancel(cancelled.id);
        Lunch planned = lunches.plan(TODAY.plusDays(7));

        assertThat(lunches.planned()).containsSame(planned);
    }

    @Test
    void cancellingMakesItACancelledLunch() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.cancel(lunch.id);

        assertThat(lunch.state).isEqualTo(LunchState.CANCELLED);
        assertThat(lunch.isPlanned()).isFalse();
        assertThat(lunches.planned()).isEmpty();
        assertThat(lunches.lunch(lunch.id)).isSameAs(lunch);
    }

    @Test
    void aCancelledLunchMakesRoomForTheNextOne() {
        Lunch cancelled = lunches.plan(TODAY.plusDays(7));
        lunches.cancel(cancelled.id);

        Lunch next = lunches.plan(TODAY.plusDays(14));

        assertThat(lunches.planned()).containsSame(next);
    }

    @Test
    void aCancelledLunchCannotBeCancelledAgain() {
        Lunch lunch = lunches.plan(TODAY);
        lunches.cancel(lunch.id);

        assertThatExceptionOfType(LunchNotPlannedException.class).isThrownBy(() -> lunches.cancel(lunch.id));
    }

    @Test
    void anUnknownLunchCannotBeCancelled() {
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class).isThrownBy(() -> lunches.cancel(unknown));
    }

    @Test
    void anUnknownLunchIsNotFound() {
        lunches.plan(TODAY);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class).isThrownBy(() -> lunches.lunch(unknown));
    }

    @Test
    void listsThePlannedLunchFirstThenTheOthersLatestFirst() {
        Lunch early = planAndCancel(TODAY.plusDays(1));
        Lunch late = planAndCancel(TODAY.plusDays(20));
        Lunch middle = planAndCancel(TODAY.plusDays(10));
        Lunch planned = lunches.plan(TODAY.plusDays(5));

        assertThat(lunches.all()).containsExactly(planned, late, middle, early);
    }

    @Test
    void listsLunchesOnTheSameDateLatestPlannedFirst() {
        Lunch first = planAndCancel(TODAY);
        clock.advance(Duration.ofMinutes(1));
        Lunch second = planAndCancel(TODAY);
        clock.advance(Duration.ofMinutes(1));
        Lunch third = planAndCancel(TODAY);

        assertThat(lunches.all()).containsExactly(third, second, first);
    }

    @Test
    void listsLunchesPlannedAtTheSameMomentInAStableOrder() {
        Lunch first = planAndCancel(TODAY);
        Lunch second = planAndCancel(TODAY);

        assertThat(lunches.all())
                .containsExactlyElementsOf(Stream.of(first, second)
                        .sorted(Comparator.comparing((Lunch l) -> l.id))
                        .toList());
    }

    @Test
    void commentingStoresTheAuthorTextAndTime() {
        Lunch lunch = lunches.plan(TODAY);

        LunchComment comment = lunches.comment(lunch.id, anna, "Only if we stay near Barfi");

        assertThat(comment.id).isNotNull();
        assertThat(comment.lunch).isSameAs(lunch);
        assertThat(comment.author).isSameAs(anna);
        assertThat(comment.text).isEqualTo("Only if we stay near Barfi");
        assertThat(comment.writtenAt).isEqualTo(NOW);
        assertThat(lunch.chat()).containsExactly(comment);
        assertThat(repository.comments()).containsExactly(comment);
    }

    @Test
    void stripsTheComment() {
        Lunch lunch = lunches.plan(TODAY);

        assertThat(lunches.comment(lunch.id, anna, " \n Barfi suits me\t").text).isEqualTo("Barfi suits me");
    }

    @Test
    void rejectsABlankComment() {
        Lunch lunch = lunches.plan(TODAY);

        assertThatExceptionOfType(CommentTextRequiredException.class)
                .isThrownBy(() -> lunches.comment(lunch.id, anna, " \n\t "));
        assertThatExceptionOfType(CommentTextRequiredException.class)
                .isThrownBy(() -> lunches.comment(lunch.id, anna, null));
        assertThat(lunch.chat()).isEmpty();
        assertThat(repository.comments()).isEmpty();
    }

    @Test
    void anUnknownLunchCannotBeCommentedOn() {
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.comment(unknown, anna, "Hello"));
    }

    @Test
    void aCancelledLunchCanBeCommentedOn() {
        Lunch lunch = planAndCancel(TODAY);

        lunches.comment(lunch.id, anna, "Next time then");

        assertThat(lunch.chat()).extracting(c -> c.text).containsExactly("Next time then");
    }

    @Test
    void theChatReadsOldestFirst() {
        Lunch lunch = lunches.plan(TODAY);
        LunchComment first = lunches.comment(lunch.id, anna, "Only if we stay near Barfi");
        clock.advance(Duration.ofMinutes(5));
        LunchComment second = lunches.comment(lunch.id, ben, "Barfi suits me");

        assertThat(lunches.lunch(lunch.id).chat()).containsExactly(first, second);
    }

    private Lunch planAndCancel(LocalDate date) {
        Lunch lunch = lunches.plan(date);
        lunches.cancel(lunch.id);
        return lunch;
    }

    private static Member member(String displayName) {
        Member member = new Member();
        member.id = UUID.randomUUID();
        member.displayName = displayName;
        return member;
    }
}
