package ch.kup.foodnomads.lunches;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** A {@link LunchRepository} for unit tests. */
class InMemoryLunchRepository implements LunchRepository {

    private final List<Lunch> lunches = new ArrayList<>();
    private final List<LunchComment> comments = new ArrayList<>();

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

    List<LunchComment> comments() {
        return List.copyOf(comments);
    }
}
