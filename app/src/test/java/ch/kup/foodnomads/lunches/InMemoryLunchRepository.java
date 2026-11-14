package ch.kup.foodnomads.lunches;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A {@link LunchRepository} for unit tests. */
class InMemoryLunchRepository implements LunchRepository {

    private final List<Lunch> lunches = new ArrayList<>();
    private final List<LunchComment> comments = new ArrayList<>();
    private final List<CandidateVenue> candidates = new ArrayList<>();
    private final List<UUID> locked = new ArrayList<>();

    @Override
    public void add(Lunch lunch) {
        lunches.add(lunch);
    }

    @Override
    public Optional<Lunch> find(UUID id) {
        return lunches.stream().filter(lunch -> lunch.id.equals(id)).findFirst();
    }

    @Override
    public Optional<Lunch> findForUpdate(UUID id) {
        locked.add(id);
        return find(id);
    }

    @Override
    public Optional<Lunch> planned() {
        return lunches.stream().filter(Lunch::isPlanned).findFirst();
    }

    @Override
    public List<Lunch> all() {
        return List.copyOf(lunches);
    }

    @Override
    public void addComment(LunchComment comment) {
        comments.add(comment);
    }

    @Override
    public void addCandidate(CandidateVenue candidate) {
        candidates.add(candidate);
    }

    @Override
    public void removeCandidate(CandidateVenue candidate) {
        candidates.remove(candidate);
    }

    List<LunchComment> comments() {
        return List.copyOf(comments);
    }

    List<CandidateVenue> candidates() {
        return List.copyOf(candidates);
    }

    /** The ids {@link #findForUpdate} was asked for, in order. */
    List<UUID> locked() {
        return List.copyOf(locked);
    }
}
