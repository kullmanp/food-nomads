package ch.kup.foodnomads.lunches;

import ch.kup.foodnomads.members.Member;
import ch.kup.foodnomads.venues.Venue;
import ch.kup.foodnomads.venues.VenueNotFoundException;
import ch.kup.foodnomads.venues.Venues;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.text.Collator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Planning, moving and cancelling Lunches, recording the ones from before the app, putting up
 * Candidate Venues and voting on them, answering in or out, and talking Lunches over in their Lunch
 * Comments. Which Venues are visited follows from the Held Lunches.
 */
@ApplicationScoped
public class Lunches {

    /** The group lunches in Basel, so that's where "today" is. */
    private static final ZoneId BASEL = ZoneId.of("Europe/Zurich");

    /** The Planned Lunch, then the others latest first. */
    private static final Comparator<Lunch> LIST_ORDER = Comparator.comparing((Lunch l) -> !l.isPlanned())
            .thenComparing((Lunch l) -> l.date, Comparator.reverseOrder())
            .thenComparing((Lunch l) -> l.createdAt, Comparator.reverseOrder())
            .thenComparing(l -> l.id);

    /** By name as a German speaker sorts it, then by id, so that equal names keep a stable order. */
    private static final Comparator<Venue> BY_NAME = Comparator.comparing(
                    (Venue v) -> v.name, Collator.getInstance(Locale.GERMAN))
            .thenComparing(v -> v.id);

    /** Up minus down, highest first, then by name. */
    private static final Comparator<ListedCandidate> CANDIDATE_ORDER = Comparator.comparingInt(
                    (ListedCandidate c) -> c.down() - c.up())
            .thenComparing(ListedCandidate::venue, BY_NAME);

    private final LunchRepository repository;
    private final Venues venues;
    private final Clock clock;

    public Lunches(LunchRepository repository, Venues venues, Clock clock) {
        this.repository = repository;
        this.venues = venues;
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
     * Records a Lunch from before the app as held, on this date at this Venue, which makes the
     * Venue visited. It isn't the Planned Lunch, so it can be recorded while one is planned. Fails
     * with {@link DateNotPastException} unless the date is before today, with
     * {@link VenueNotFoundException} for an unknown Venue and with {@link VenueVisitedException} if
     * a Lunch has been held there already.
     */
    @Transactional
    public Lunch recordPast(LocalDate date, UUID venueId) {
        if (!date.isBefore(today())) {
            throw new DateNotPastException();
        }
        // Locked before asking whether it's visited, so that two Members can't both record it.
        Venue venue = venues.lock(venueId);
        if (visitedVenueIds().contains(venue.id)) {
            throw new VenueVisitedException();
        }
        Lunch lunch = new Lunch();
        lunch.id = UUID.randomUUID();
        lunch.date = date;
        lunch.state = LunchState.HELD;
        lunch.venue = venue;
        lunch.createdAt = clock.instant();
        repository.add(lunch);
        return lunch;
    }

    /** The ids of the Visited Venues: those a Held Lunch has as its Venue. */
    @Transactional
    public Set<UUID> visitedVenueIds() {
        return repository.all().stream()
                .filter(Lunch::isHeld)
                .map(lunch -> lunch.venue.id)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** The Venues no Lunch has been held at, by name. */
    @Transactional
    public List<Venue> unvisitedVenues() {
        Set<UUID> visited = visitedVenueIds();
        return venues.newestFirst().stream()
                .filter(venue -> !visited.contains(venue.id))
                .sorted(BY_NAME)
                .toList();
    }

    /**
     * Puts this Venue up as a Candidate Venue of the Planned Lunch. Fails with
     * {@link LunchNotPlannedException} for a held or cancelled Lunch, with
     * {@link VenueNotFoundException} for an unknown Venue, with {@link VenueVisitedException} if a
     * Lunch has been held there, and with {@link AlreadyCandidateException} if it's up already.
     */
    @Transactional
    public CandidateVenue addCandidate(UUID lunchId, UUID venueId) {
        Lunch lunch = plannedForUpdate(lunchId);
        Venue venue = venues.venue(venueId);
        if (visitedVenueIds().contains(venue.id)) {
            throw new VenueVisitedException();
        }
        if (candidateOf(lunch, venueId).isPresent()) {
            throw new AlreadyCandidateException();
        }
        CandidateVenue candidate = new CandidateVenue();
        candidate.id = UUID.randomUUID();
        candidate.lunch = lunch;
        candidate.venue = venue;
        lunch.candidates.add(candidate);
        repository.addCandidate(candidate);
        return candidate;
    }

    /**
     * Takes this Venue off the Planned Lunch's Candidate Venues, also if it has become visited in
     * the meantime. Fails with {@link LunchNotPlannedException} for a held or cancelled Lunch and
     * with {@link NotACandidateException} if the Venue isn't up.
     */
    @Transactional
    public void removeCandidate(UUID lunchId, UUID venueId) {
        Lunch lunch = plannedForUpdate(lunchId);
        CandidateVenue candidate = candidateOf(lunch, venueId).orElseThrow(NotACandidateException::new);
        // Its Votes go with it.
        candidate.votes.forEach(repository::removeVote);
        lunch.candidates.remove(candidate);
        repository.removeCandidate(candidate);
    }

    /**
     * The Member votes {@code direction} on this Candidate Venue of the Planned Lunch. A Member has
     * at most one Vote per Candidate Venue, so voting again replaces it. Fails with
     * {@link LunchNotPlannedException} for a held or cancelled Lunch and with
     * {@link NotACandidateException} if the Venue isn't up.
     */
    @Transactional
    public Vote vote(Member member, UUID lunchId, UUID venueId, VoteDirection direction) {
        Lunch lunch = plannedForUpdate(lunchId);
        CandidateVenue candidate = candidateOf(lunch, venueId).orElseThrow(NotACandidateException::new);
        Optional<Vote> existing = voteOf(candidate, member);
        if (existing.isPresent()) {
            existing.get().direction = direction;
            return existing.get();
        }
        Vote vote = new Vote();
        vote.id = UUID.randomUUID();
        vote.candidate = candidate;
        vote.member = member;
        vote.direction = direction;
        candidate.votes.add(vote);
        repository.addVote(vote);
        return vote;
    }

    /**
     * Takes the Member's Vote off this Candidate Venue of the Planned Lunch. Without one, nothing
     * changes. Fails with {@link LunchNotPlannedException} for a held or cancelled Lunch and with
     * {@link NotACandidateException} if the Venue isn't up.
     */
    @Transactional
    public void withdrawVote(Member member, UUID lunchId, UUID venueId) {
        Lunch lunch = plannedForUpdate(lunchId);
        CandidateVenue candidate = candidateOf(lunch, venueId).orElseThrow(NotACandidateException::new);
        voteOf(candidate, member).ifPresent(vote -> {
            candidate.votes.remove(vote);
            repository.removeVote(vote);
        });
    }

    /**
     * The Lunch's Candidate Venues as this Member sees them: each marked visited or not, with its
     * up and down Votes and the Member's own. Listed by up minus down, highest first, then by name.
     */
    @Transactional
    public List<ListedCandidate> candidates(Lunch lunch, Member member) {
        Set<UUID> visited = visitedVenueIds();
        return lunch.candidates.stream()
                .map(candidate -> new ListedCandidate(
                        candidate.venue,
                        visited.contains(candidate.venue.id),
                        count(candidate, VoteDirection.UP),
                        count(candidate, VoteDirection.DOWN),
                        voteOf(candidate, member).map(vote -> vote.direction).orElse(null)))
                .sorted(CANDIDATE_ORDER)
                .toList();
    }

    /** The Venues that can still be put up for this Lunch, by name: none unless it's planned. */
    @Transactional
    public List<Venue> addableVenues(Lunch lunch) {
        if (!lunch.isPlanned()) {
            return List.of();
        }
        return unvisitedVenues().stream()
                .filter(venue -> candidateOf(lunch, venue.id).isEmpty())
                .toList();
    }

    /**
     * The Member answers {@code in} or {@code out} for the Planned Lunch. A Member has at most one
     * RSVP per Lunch, so answering again replaces it. Fails with {@link LunchNotPlannedException}
     * for a held or cancelled Lunch.
     */
    @Transactional
    public Rsvp rsvp(Member member, UUID lunchId, RsvpAnswer answer) {
        Lunch lunch = plannedForUpdate(lunchId);
        Optional<Rsvp> existing = lunch.rsvpOf(member);
        if (existing.isPresent()) {
            existing.get().answer = answer;
            return existing.get();
        }
        Rsvp rsvp = new Rsvp();
        rsvp.id = UUID.randomUUID();
        rsvp.lunch = lunch;
        rsvp.member = member;
        rsvp.answer = answer;
        lunch.rsvps.add(rsvp);
        repository.addRsvp(rsvp);
        return rsvp;
    }

    /**
     * Moves the Planned Lunch to this date. Its RSVPs are cleared, because "in" for one day says
     * nothing about another; its comments, Candidate Venues and Votes stay. Moving it to the date
     * it has already changes nothing. Fails with {@link LunchNotPlannedException} for a held or
     * cancelled Lunch and with {@link DateInPastException} before today.
     */
    @Transactional
    public void move(UUID lunchId, LocalDate date) {
        Lunch lunch = plannedForUpdate(lunchId);
        if (date.isBefore(today())) {
            throw new DateInPastException();
        }
        if (date.equals(lunch.date)) {
            return;
        }
        lunch.date = date;
        lunch.rsvps.forEach(repository::removeRsvp);
        lunch.rsvps.clear();
    }

    /** The Lunch, held for this transaction, or {@link LunchNotPlannedException} unless it's planned. */
    private Lunch plannedForUpdate(UUID id) {
        Lunch lunch = repository.findForUpdate(id).orElseThrow(LunchNotFoundException::new);
        if (!lunch.isPlanned()) {
            throw new LunchNotPlannedException();
        }
        return lunch;
    }

    private static Optional<Vote> voteOf(CandidateVenue candidate, Member member) {
        return candidate.votes.stream()
                .filter(vote -> vote.member.id.equals(member.id))
                .findFirst();
    }

    private static int count(CandidateVenue candidate, VoteDirection direction) {
        return (int) candidate.votes.stream()
                .filter(vote -> vote.direction == direction)
                .count();
    }

    private static Optional<CandidateVenue> candidateOf(Lunch lunch, UUID venueId) {
        return lunch.candidates.stream()
                .filter(candidate -> candidate.venue.id.equals(venueId))
                .findFirst();
    }

    /**
     * Calls the Planned Lunch off. It stays, as a Cancelled Lunch. Fails with
     * {@link LunchNotPlannedException} if it is cancelled already.
     */
    @Transactional
    public void cancel(UUID id) {
        plannedForUpdate(id).state = LunchState.CANCELLED;
    }

    @Transactional
    public Optional<Lunch> planned() {
        return repository.planned();
    }

    /**
     * The Lunch with its Venue, comments and their authors, and Candidate Venues loaded, or
     * {@link LunchNotFoundException}.
     */
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
