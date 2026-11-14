package ch.kup.foodnomads.lunches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.members.MutableClock;
import ch.kup.foodnomads.venues.InMemoryVenueRepository;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.VenueNotFoundException;
import ch.kup.foodnomads.venues.Venues;
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
    private final InMemoryVenueRepository venueRepository = new InMemoryVenueRepository();
    private final Venues venues = new Venues(venueRepository);
    private final Lunches lunches = new Lunches(repository, venues, clock);

    private final Member anna = member("Anna");
    private final Member ben = member("Ben");
    private final Member cleo = member("Cleo");

    @Test
    void planningStoresAPlannedLunchOnThatDate() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        assertThat(lunch.id).isNotNull();
        assertThat(lunch.date).isEqualTo(TODAY.plusDays(7));
        assertThat(lunch.state).isEqualTo(LunchState.PLANNED);
        assertThat(lunch.isPlanned()).isTrue();
        assertThat(lunch.createdAt).isEqualTo(NOW);
        assertThat(lunch.comments).isEmpty();
        assertThat(lunch.candidates).isEmpty();
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
        Lunches inSummer = new Lunches(repository, venues, summer);
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

    @Test
    void recordingStoresAHeldLunchAtThatVenue() {
        Venue sternen = venues.propose(anna, "Sternen", null);

        Lunch lunch = lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThat(lunch.id).isNotNull();
        assertThat(lunch.date).isEqualTo(TODAY.minusDays(60));
        assertThat(lunch.state).isEqualTo(LunchState.HELD);
        assertThat(lunch.isHeld()).isTrue();
        assertThat(lunch.isPlanned()).isFalse();
        assertThat(lunch.venue).isSameAs(sternen);
        assertThat(lunch.createdAt).isEqualTo(NOW);
        assertThat(lunch.comments).isEmpty();
        assertThat(repository.all()).containsExactly(lunch);
    }

    @Test
    void recordingLocksTheVenue() {
        Venue sternen = venues.propose(anna, "Sternen", null);

        lunches.recordPast(TODAY.minusDays(1), sternen.id);

        assertThat(venueRepository.locked()).containsExactly(sternen.id);
    }

    @Test
    void aLunchFromYesterdayCanBeRecorded() {
        Venue sternen = venues.propose(anna, "Sternen", null);

        assertThat(lunches.recordPast(TODAY.minusDays(1), sternen.id).date).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void aLunchFromTodayOrLaterCannotBeRecorded() {
        Venue sternen = venues.propose(anna, "Sternen", null);

        assertThatExceptionOfType(DateNotPastException.class).isThrownBy(() -> lunches.recordPast(TODAY, sternen.id));
        assertThatExceptionOfType(DateNotPastException.class)
                .isThrownBy(() -> lunches.recordPast(TODAY.plusDays(1), sternen.id));
        assertThat(repository.all()).isEmpty();
        assertThat(lunches.visitedVenueIds()).isEmpty();
    }

    @Test
    void justBeforeMidnightInBaselTodayCannotBeRecordedYet() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        // 23:59:59.999999999 in Basel (UTC+1 in winter) on 1 March.
        clock.advance(Duration.ofHours(13).minusNanos(1));

        assertThatExceptionOfType(DateNotPastException.class).isThrownBy(() -> lunches.recordPast(TODAY, sternen.id));
        assertThat(lunches.recordPast(TODAY.minusDays(1), sternen.id).date).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    void atMidnightInBaselTheDayBeforeCanBeRecorded() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        // 00:00 in Basel on 2 March, while it is still 1 March in UTC.
        clock.advance(Duration.ofHours(13));

        assertThat(lunches.recordPast(TODAY, sternen.id).date).isEqualTo(TODAY);
    }

    @Test
    void aLunchAtAnUnknownVenueCannotBeRecorded() {
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(VenueNotFoundException.class)
                .isThrownBy(() -> lunches.recordPast(TODAY.minusDays(1), unknown));
        assertThat(repository.all()).isEmpty();
    }

    @Test
    void aVisitedVenueCannotBeRecordedAgain() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch spring = lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThatExceptionOfType(VenueVisitedException.class)
                .isThrownBy(() -> lunches.recordPast(TODAY.minusDays(30), sternen.id));
        assertThat(repository.all()).containsExactly(spring);
        assertThat(venueRepository.locked()).containsExactly(sternen.id, sternen.id);
    }

    @Test
    void anotherVenueCanBeRecordedAfterOneIsVisited() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThat(lunches.recordPast(TODAY.minusDays(30), roessli.id).venue).isSameAs(roessli);
    }

    @Test
    void aPastLunchCanBeRecordedWhileALunchIsPlanned() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch planned = lunches.plan(TODAY.plusDays(7));

        Lunch held = lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThat(lunches.planned()).containsSame(planned);
        assertThat(lunches.all()).containsExactly(planned, held);
    }

    @Test
    void aRecordedLunchCanBeCommentedOn() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.recordPast(TODAY.minusDays(60), sternen.id);

        lunches.comment(lunch.id, anna, "The best Cordon bleu so far");

        assertThat(lunches.lunch(lunch.id).chat())
                .extracting(c -> c.text)
                .containsExactly("The best Cordon bleu so far");
    }

    @Test
    void noVenueIsVisitedAtFirst() {
        venues.propose(anna, "Sternen", null);
        lunches.plan(TODAY);

        assertThat(lunches.visitedVenueIds()).isEmpty();
    }

    @Test
    void aVenueIsVisitedOnceAHeldLunchHasIt() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        venues.propose(anna, "Rössli", null);
        lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThat(lunches.visitedVenueIds()).containsExactly(sternen.id);
    }

    @Test
    void onlyAHeldLunchMakesItsVenueVisited() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch cancelled = new Lunch();
        cancelled.id = UUID.randomUUID();
        cancelled.date = TODAY;
        cancelled.state = LunchState.CANCELLED;
        cancelled.venue = sternen;
        repository.add(cancelled);

        assertThat(lunches.visitedVenueIds()).isEmpty();
        assertThat(lunches.unvisitedVenues()).containsExactly(sternen);
        assertThat(lunches.recordPast(TODAY.minusDays(1), sternen.id).venue).isSameAs(sternen);
    }

    @Test
    void offersTheUnvisitedVenuesByName() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        Venue ankerLower = venues.propose(anna, "anker", null);
        Venue zum = venues.propose(anna, "Zum Bären", null);
        // Ids in the opposite order, so that only the names can put them in order.
        sternen.id = UUID.fromString("00000000-0000-0000-0000-000000000001");
        roessli.id = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ankerLower.id = UUID.fromString("00000000-0000-0000-0000-000000000003");
        lunches.recordPast(TODAY.minusDays(60), zum.id);

        assertThat(lunches.unvisitedVenues()).containsExactly(ankerLower, roessli, sternen);
    }

    @Test
    void offersVenuesWithTheSameNameInAStableOrder() {
        Venue first = venues.propose(anna, "Sternen", null);
        Venue second = venues.propose(anna, "Sternen", null);

        assertThat(lunches.unvisitedVenues())
                .containsExactlyElementsOf(Stream.of(first, second)
                        .sorted(Comparator.comparing((Venue v) -> v.id))
                        .toList());
    }

    @Test
    void puttingUpAVenueMakesItACandidateVenueOfThePlannedLunch() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);

        assertThat(candidate.id).isNotNull();
        assertThat(candidate.lunch).isSameAs(lunch);
        assertThat(candidate.venue).isSameAs(sternen);
        assertThat(lunch.candidates).containsExactly(candidate);
        assertThat(repository.candidates()).containsExactly(candidate);
        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, false, 0, 0, null));
    }

    @Test
    void aNewPlannedLunchHasNoCandidateVenues() {
        venues.propose(anna, "Sternen", null);

        assertThat(lunches.candidates(lunches.plan(TODAY), anna)).isEmpty();
    }

    @Test
    void aVisitedVenueCannotBeACandidateVenue() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        lunches.recordPast(TODAY.minusDays(60), sternen.id);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        assertThatExceptionOfType(VenueVisitedException.class)
                .isThrownBy(() -> lunches.addCandidate(lunch.id, sternen.id));
        assertThat(lunch.candidates).isEmpty();
        assertThat(repository.candidates()).isEmpty();
    }

    @Test
    void aVenueIsACandidateVenueOnlyOnce() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);

        assertThatExceptionOfType(AlreadyCandidateException.class)
                .isThrownBy(() -> lunches.addCandidate(lunch.id, sternen.id));
        assertThat(lunch.candidates).containsExactly(candidate);
        assertThat(repository.candidates()).containsExactly(candidate);
    }

    @Test
    void onlyThePlannedLunchTakesCandidateVenues() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        Lunch cancelled = planAndCancel(TODAY);
        Lunch held = lunches.recordPast(TODAY.minusDays(60), roessli.id);

        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.addCandidate(cancelled.id, sternen.id));
        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.addCandidate(held.id, sternen.id));
        assertThat(repository.candidates()).isEmpty();
    }

    @Test
    void anUnknownLunchTakesNoCandidateVenues() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.addCandidate(unknown, sternen.id));
    }

    @Test
    void anUnknownVenueCannotBeACandidateVenue() {
        Lunch lunch = lunches.plan(TODAY);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(VenueNotFoundException.class)
                .isThrownBy(() -> lunches.addCandidate(lunch.id, unknown));
        assertThat(repository.candidates()).isEmpty();
    }

    @Test
    void puttingUpACandidateVenueLocksThePlannedLunch() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY);

        lunches.addCandidate(lunch.id, sternen.id);

        assertThat(repository.locked()).containsExactly(lunch.id);
    }

    @Test
    void removingACandidateVenueTakesItOffTheLunch() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue kept = lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, kunsthalle.id);

        lunches.removeCandidate(lunch.id, kunsthalle.id);

        assertThat(lunch.candidates).containsExactly(kept);
        assertThat(repository.candidates()).containsExactly(kept);
        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, false, 0, 0, null));
        assertThat(repository.locked()).containsExactly(lunch.id, lunch.id, lunch.id);
    }

    @Test
    void aRemovedCandidateVenueCanBePutUpAgain() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.removeCandidate(lunch.id, sternen.id);

        CandidateVenue again = lunches.addCandidate(lunch.id, sternen.id);

        assertThat(lunch.candidates).containsExactly(again);
    }

    @Test
    void aVenueThatIsNoCandidateVenueCannotBeRemoved() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);

        assertThatExceptionOfType(NotACandidateException.class)
                .isThrownBy(() -> lunches.removeCandidate(lunch.id, kunsthalle.id));
        assertThat(lunch.candidates).containsExactly(candidate);
        assertThat(repository.candidates()).containsExactly(candidate);
    }

    @Test
    void candidateVenuesOfACancelledLunchStayAndCannotBeRemoved() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);
        lunches.cancel(lunch.id);

        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.removeCandidate(lunch.id, sternen.id));
        assertThat(lunch.candidates).containsExactly(candidate);
        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, false, 0, 0, null));
    }

    @Test
    void anUnknownLunchHasNoCandidateVenuesToRemove() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.removeCandidate(unknown, sternen.id));
    }

    @Test
    void aCandidateVenueThatBecomesVisitedStaysListedAsVisited() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);

        lunches.recordPast(TODAY.minusDays(60), sternen.id);

        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, true, 0, 0, null));
    }

    @Test
    void aCandidateVenueThatBecameVisitedCanBeRemoved() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.recordPast(TODAY.minusDays(60), sternen.id);

        lunches.removeCandidate(lunch.id, sternen.id);

        assertThat(lunches.candidates(lunch, anna)).isEmpty();
    }

    @Test
    void listsCandidateVenuesByName() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        Venue ankerLower = venues.propose(anna, "anker", null);
        Lunch lunch = lunches.plan(TODAY);
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, roessli.id);
        lunches.addCandidate(lunch.id, ankerLower.id);

        assertThat(lunches.candidates(lunch, anna))
                .extracting(ListedCandidate::venue)
                .containsExactly(ankerLower, roessli, sternen);
    }

    @Test
    void listsCandidateVenuesWithTheSameNameInAStableOrder() {
        Venue first = venues.propose(anna, "Sternen", null);
        Venue second = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY);
        lunches.addCandidate(lunch.id, second.id);
        lunches.addCandidate(lunch.id, first.id);

        assertThat(lunches.candidates(lunch, anna))
                .extracting(ListedCandidate::venue)
                .containsExactlyElementsOf(Stream.of(first, second)
                        .sorted(Comparator.comparing((Venue v) -> v.id))
                        .toList());
    }

    @Test
    void offersTheUnvisitedVenuesThatAreNoCandidateVenuesYet() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Venue anker = venues.propose(anna, "Anker", null);
        lunches.recordPast(TODAY.minusDays(60), roessli.id);
        Lunch lunch = lunches.plan(TODAY);
        lunches.addCandidate(lunch.id, kunsthalle.id);

        assertThat(lunches.addableVenues(lunch)).containsExactly(anker, sternen);
    }

    @Test
    void offersNoVenuesForALunchThatIsNotPlanned() {
        venues.propose(anna, "Sternen", null);
        Lunch cancelled = planAndCancel(TODAY);

        assertThat(lunches.addableVenues(cancelled)).isEmpty();
    }

    @Test
    void votingUpCountsForTheCandidateVenue() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);

        Vote vote = lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);

        assertThat(vote.id).isNotNull();
        assertThat(vote.candidate).isSameAs(candidate);
        assertThat(vote.member).isSameAs(anna);
        assertThat(vote.direction).isEqualTo(VoteDirection.UP);
        assertThat(candidate.votes).containsExactly(vote);
        assertThat(repository.votes()).containsExactly(vote);
        assertThat(lunches.candidates(lunch, anna))
                .containsExactly(new ListedCandidate(sternen, false, 1, 0, VoteDirection.UP));
    }

    @Test
    void votesOfAllMembersAreCountedButOnlyTheirOwnIsShownAsTheirs() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, kunsthalle.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.vote(ben, lunch.id, sternen.id, VoteDirection.UP);
        lunches.vote(anna, lunch.id, kunsthalle.id, VoteDirection.DOWN);

        assertThat(lunches.candidates(lunch, ben))
                .containsExactly(
                        new ListedCandidate(sternen, false, 2, 0, VoteDirection.UP),
                        new ListedCandidate(kunsthalle, false, 0, 1, null));
    }

    @Test
    void votingAgainReplacesTheVote() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);
        Vote first = lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);

        Vote again = lunches.vote(anna, lunch.id, sternen.id, VoteDirection.DOWN);

        assertThat(again).isSameAs(first);
        assertThat(again.direction).isEqualTo(VoteDirection.DOWN);
        assertThat(candidate.votes).containsExactly(first);
        assertThat(repository.votes()).containsExactly(first);
        assertThat(lunches.candidates(lunch, anna))
                .containsExactly(new ListedCandidate(sternen, false, 0, 1, VoteDirection.DOWN));
    }

    @Test
    void aMemberIsKnownByTheirIdWhenVotingAgain() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        Member annaAgain = new Member();
        annaAgain.id = anna.id;
        annaAgain.displayName = anna.displayName;

        lunches.vote(annaAgain, lunch.id, sternen.id, VoteDirection.DOWN);

        assertThat(repository.votes()).hasSize(1);
        assertThat(lunches.candidates(lunch, annaAgain))
                .containsExactly(new ListedCandidate(sternen, false, 0, 1, VoteDirection.DOWN));
    }

    @Test
    void votingLocksThePlannedLunch() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY);
        lunches.addCandidate(lunch.id, sternen.id);

        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.withdrawVote(anna, lunch.id, sternen.id);

        assertThat(repository.locked()).containsExactly(lunch.id, lunch.id, lunch.id);
    }

    @Test
    void onlyCandidateVenuesTakeVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);

        assertThatExceptionOfType(NotACandidateException.class)
                .isThrownBy(() -> lunches.vote(anna, lunch.id, kunsthalle.id, VoteDirection.UP));
        assertThat(repository.votes()).isEmpty();
    }

    @Test
    void onlyThePlannedLunchTakesVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.cancel(lunch.id);

        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.vote(anna, lunch.id, sternen.id, VoteDirection.DOWN));
        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.vote(ben, lunch.id, sternen.id, VoteDirection.UP));
        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.withdrawVote(anna, lunch.id, sternen.id));
        assertThat(lunches.candidates(lunch, anna))
                .containsExactly(new ListedCandidate(sternen, false, 1, 0, VoteDirection.UP));
    }

    @Test
    void anUnknownLunchTakesNoVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.vote(anna, unknown, sternen.id, VoteDirection.UP));
        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.withdrawVote(anna, unknown, sternen.id));
    }

    @Test
    void withdrawingTakesTheVoteAway() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        Vote kept = lunches.vote(ben, lunch.id, sternen.id, VoteDirection.UP);

        lunches.withdrawVote(anna, lunch.id, sternen.id);

        assertThat(candidate.votes).containsExactly(kept);
        assertThat(repository.votes()).containsExactly(kept);
        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, false, 1, 0, null));
    }

    @Test
    void withdrawingWithoutAVoteChangesNothing() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        Vote kept = lunches.vote(ben, lunch.id, sternen.id, VoteDirection.DOWN);

        lunches.withdrawVote(anna, lunch.id, sternen.id);

        assertThat(repository.votes()).containsExactly(kept);
    }

    @Test
    void aVenueThatIsNoCandidateVenueHasNoVoteToWithdraw() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        assertThatExceptionOfType(NotACandidateException.class)
                .isThrownBy(() -> lunches.withdrawVote(anna, lunch.id, sternen.id));
    }

    @Test
    void removingACandidateVenueRemovesItsVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, kunsthalle.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.vote(ben, lunch.id, sternen.id, VoteDirection.DOWN);
        Vote kept = lunches.vote(anna, lunch.id, kunsthalle.id, VoteDirection.UP);

        lunches.removeCandidate(lunch.id, sternen.id);

        assertThat(repository.votes()).containsExactly(kept);
    }

    @Test
    void aCandidateVenuePutUpAgainStartsWithoutVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.removeCandidate(lunch.id, sternen.id);

        lunches.addCandidate(lunch.id, sternen.id);

        assertThat(lunches.candidates(lunch, anna)).containsExactly(new ListedCandidate(sternen, false, 0, 0, null));
    }

    @Test
    void listsCandidateVenuesByUpMinusDownHighestFirst() {
        Venue anker = venues.propose(anna, "Anker", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Venue roessli = venues.propose(anna, "Rössli", null);
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY);
        Stream.of(anker, kunsthalle, roessli, sternen).forEach(v -> lunches.addCandidate(lunch.id, v.id));
        // Kunsthalle 2 - 1 and Sternen 1 - 0 tie at 1, so by name. Then Rössli at 0, Anker at -1.
        lunches.vote(anna, lunch.id, anker.id, VoteDirection.DOWN);
        lunches.vote(anna, lunch.id, kunsthalle.id, VoteDirection.UP);
        lunches.vote(ben, lunch.id, kunsthalle.id, VoteDirection.UP);
        lunches.vote(cleo, lunch.id, kunsthalle.id, VoteDirection.DOWN);
        lunches.vote(ben, lunch.id, sternen.id, VoteDirection.UP);

        assertThat(lunches.candidates(lunch, anna))
                .extracting(ListedCandidate::venue)
                .containsExactly(kunsthalle, sternen, roessli, anker);
    }

    @Test
    void listsCandidateVenuesWithTheSameBalanceByName() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue anker = venues.propose(anna, "Anker", null);
        Lunch lunch = lunches.plan(TODAY);
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, anker.id);
        // Both 1 - 1 = 0, though Sternen has more up Votes.
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);
        lunches.vote(ben, lunch.id, sternen.id, VoteDirection.DOWN);

        assertThat(lunches.candidates(lunch, anna))
                .extracting(ListedCandidate::venue)
                .containsExactly(anker, sternen);
    }

    @Test
    void votesStayVisibleOnceTheLunchIsCancelled() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.DOWN);

        lunches.cancel(lunch.id);

        assertThat(lunches.candidates(lunch, anna))
                .containsExactly(new ListedCandidate(sternen, false, 0, 1, VoteDirection.DOWN));
    }

    @Test
    void aVoteForOneCandidateVenueIsNotAVoteForAnother() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Venue kunsthalle = venues.propose(anna, "Kunsthalle", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.addCandidate(lunch.id, sternen.id);
        lunches.addCandidate(lunch.id, kunsthalle.id);
        lunches.vote(anna, lunch.id, sternen.id, VoteDirection.UP);

        lunches.vote(anna, lunch.id, kunsthalle.id, VoteDirection.DOWN);
        lunches.withdrawVote(anna, lunch.id, kunsthalle.id);

        assertThat(lunches.candidates(lunch, anna))
                .containsExactly(
                        new ListedCandidate(sternen, false, 1, 0, VoteDirection.UP),
                        new ListedCandidate(kunsthalle, false, 0, 0, null));
    }

    @Test
    void answeringInStoresTheMembersRsvp() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        Rsvp rsvp = lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);

        assertThat(rsvp.id).isNotNull();
        assertThat(rsvp.lunch).isSameAs(lunch);
        assertThat(rsvp.member).isSameAs(anna);
        assertThat(rsvp.answer).isEqualTo(RsvpAnswer.IN);
        assertThat(lunch.rsvps).containsExactly(rsvp);
        assertThat(repository.rsvps()).containsExactly(rsvp);
    }

    @Test
    void aNewPlannedLunchHasNoRsvps() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        assertThat(lunch.rsvps).isEmpty();
        assertThat(lunch.answered(RsvpAnswer.IN)).isEmpty();
        assertThat(lunch.answered(RsvpAnswer.OUT)).isEmpty();
    }

    @Test
    void eachMemberAnswersForThemselves() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);
        lunches.rsvp(ben, lunch.id, RsvpAnswer.OUT);
        lunches.rsvp(cleo, lunch.id, RsvpAnswer.IN);

        assertThat(lunch.answered(RsvpAnswer.IN)).containsExactly(anna, cleo);
        assertThat(lunch.answered(RsvpAnswer.OUT)).containsExactly(ben);
        assertThat(repository.rsvps()).hasSize(3);
    }

    @Test
    void answeringAgainReplacesTheAnswer() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        Rsvp first = lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);

        Rsvp again = lunches.rsvp(anna, lunch.id, RsvpAnswer.OUT);

        assertThat(again).isSameAs(first);
        assertThat(again.answer).isEqualTo(RsvpAnswer.OUT);
        assertThat(lunch.rsvps).containsExactly(first);
        assertThat(repository.rsvps()).containsExactly(first);
    }

    @Test
    void aMemberIsKnownByTheirIdWhenAnsweringAgain() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);
        Member annaAgain = new Member();
        annaAgain.id = anna.id;
        annaAgain.displayName = anna.displayName;

        lunches.rsvp(annaAgain, lunch.id, RsvpAnswer.OUT);

        assertThat(repository.rsvps()).hasSize(1);
        assertThat(lunch.answerOf(annaAgain)).isEqualTo(RsvpAnswer.OUT);
    }

    @Test
    void answeringLocksThePlannedLunch() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);

        assertThat(repository.locked()).containsExactly(lunch.id);
    }

    @Test
    void onlyThePlannedLunchTakesRsvps() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);
        lunches.cancel(lunch.id);

        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.rsvp(anna, lunch.id, RsvpAnswer.OUT));
        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.rsvp(ben, lunch.id, RsvpAnswer.IN));
        assertThat(lunch.answered(RsvpAnswer.IN)).containsExactly(anna);
        assertThat(lunch.answered(RsvpAnswer.OUT)).isEmpty();
    }

    @Test
    void anUnknownLunchTakesNoRsvps() {
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.rsvp(anna, unknown, RsvpAnswer.IN));
        assertThat(repository.rsvps()).isEmpty();
    }

    @Test
    void movingChangesTheDate() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.move(lunch.id, TODAY.plusDays(8));

        assertThat(lunch.date).isEqualTo(TODAY.plusDays(8));
        assertThat(lunch.isPlanned()).isTrue();
        assertThat(lunches.planned()).containsSame(lunch);
    }

    @Test
    void movingClearsTheRsvps() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);
        lunches.rsvp(ben, lunch.id, RsvpAnswer.OUT);

        lunches.move(lunch.id, TODAY.plusDays(8));

        assertThat(lunch.rsvps).isEmpty();
        assertThat(repository.rsvps()).isEmpty();
    }

    @Test
    void movingKeepsTheCommentsCandidateVenuesAndVotes() {
        Venue sternen = venues.propose(anna, "Sternen", null);
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        LunchComment comment = lunches.comment(lunch.id, anna, "Friday is bad for me after all");
        CandidateVenue candidate = lunches.addCandidate(lunch.id, sternen.id);
        Vote vote = lunches.vote(ben, lunch.id, sternen.id, VoteDirection.UP);

        lunches.move(lunch.id, TODAY.plusDays(8));

        assertThat(lunch.comments).containsExactly(comment);
        assertThat(repository.comments()).containsExactly(comment);
        assertThat(lunch.candidates).containsExactly(candidate);
        assertThat(repository.candidates()).containsExactly(candidate);
        assertThat(candidate.votes).containsExactly(vote);
        assertThat(repository.votes()).containsExactly(vote);
        assertThat(lunches.candidates(lunch, ben))
                .containsExactly(new ListedCandidate(sternen, false, 1, 0, VoteDirection.UP));
    }

    @Test
    void movingToTheSameDateKeepsTheRsvps() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        Rsvp rsvp = lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);

        lunches.move(lunch.id, TODAY.plusDays(7));

        assertThat(lunch.rsvps).containsExactly(rsvp);
        assertThat(repository.rsvps()).containsExactly(rsvp);
    }

    @Test
    void aLunchCanBeMovedToToday() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.move(lunch.id, TODAY);

        assertThat(lunch.date).isEqualTo(TODAY);
    }

    @Test
    void aLunchCannotBeMovedBeforeToday() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        Rsvp rsvp = lunches.rsvp(anna, lunch.id, RsvpAnswer.IN);

        assertThatExceptionOfType(DateInPastException.class)
                .isThrownBy(() -> lunches.move(lunch.id, TODAY.minusDays(1)));
        assertThat(lunch.date).isEqualTo(TODAY.plusDays(7));
        assertThat(repository.rsvps()).containsExactly(rsvp);
    }

    @Test
    void justBeforeMidnightInBaselALunchCanStillBeMovedToToday() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        // 23:59:59.999999999 in Basel (UTC+1 in winter) on 1 March.
        clock.advance(Duration.ofHours(13).minusNanos(1));

        lunches.move(lunch.id, TODAY);

        assertThat(lunch.date).isEqualTo(TODAY);
    }

    @Test
    void atMidnightInBaselALunchCannotBeMovedToTheDayBefore() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));
        // 00:00 in Basel on 2 March, while it is still 1 March in UTC.
        clock.advance(Duration.ofHours(13));

        assertThatExceptionOfType(DateInPastException.class).isThrownBy(() -> lunches.move(lunch.id, TODAY));
        lunches.move(lunch.id, TODAY.plusDays(1));
        assertThat(lunch.date).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void movingLocksThePlannedLunch() {
        Lunch lunch = lunches.plan(TODAY.plusDays(7));

        lunches.move(lunch.id, TODAY.plusDays(8));

        assertThat(repository.locked()).containsExactly(lunch.id);
    }

    @Test
    void onlyThePlannedLunchMoves() {
        Lunch lunch = planAndCancel(TODAY.plusDays(7));

        assertThatExceptionOfType(LunchNotPlannedException.class)
                .isThrownBy(() -> lunches.move(lunch.id, TODAY.plusDays(8)));
        assertThat(lunch.date).isEqualTo(TODAY.plusDays(7));
    }

    @Test
    void anUnknownLunchCannotBeMoved() {
        UUID unknown = UUID.randomUUID();

        assertThatExceptionOfType(LunchNotFoundException.class)
                .isThrownBy(() -> lunches.move(unknown, TODAY.plusDays(8)));
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
