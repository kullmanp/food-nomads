package ch.kup.foodnomads.lunches;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LunchTest {

    private static final Instant NOON = Instant.parse("2026-03-01T11:00:00Z");

    @Test
    void theChatReadsOldestFirstWhateverOrderTheCommentsCameIn() {
        Lunch lunch = new Lunch();
        LunchComment late = comment(NOON.plusSeconds(60));
        LunchComment early = comment(NOON);
        LunchComment middle = comment(NOON.plusSeconds(30));
        lunch.comments = new ArrayList<>(List.of(late, early, middle));

        assertThat(lunch.chat()).containsExactly(early, middle, late);
    }

    @Test
    void commentsWrittenAtTheSameTimeKeepAStableOrder() {
        Lunch lunch = new Lunch();
        LunchComment a = comment(NOON, "00000000-0000-0000-0000-000000000001");
        LunchComment b = comment(NOON, "00000000-0000-0000-0000-000000000002");
        lunch.comments = new ArrayList<>(List.of(b, a));

        assertThat(lunch.chat()).containsExactly(a, b);
    }

    @Test
    void onlyAPlannedLunchIsPlanned() {
        Lunch lunch = new Lunch();
        lunch.state = LunchState.PLANNED;
        assertThat(lunch.isPlanned()).isTrue();

        lunch.state = LunchState.CANCELLED;
        assertThat(lunch.isPlanned()).isFalse();
    }

    @Test
    void onlyAHeldLunchIsHeld() {
        Lunch lunch = new Lunch();
        lunch.state = LunchState.HELD;
        assertThat(lunch.isHeld()).isTrue();
        assertThat(lunch.isPlanned()).isFalse();

        lunch.state = LunchState.PLANNED;
        assertThat(lunch.isHeld()).isFalse();

        lunch.state = LunchState.CANCELLED;
        assertThat(lunch.isHeld()).isFalse();
    }

    private static LunchComment comment(Instant writtenAt) {
        return comment(writtenAt, UUID.randomUUID().toString());
    }

    private static LunchComment comment(Instant writtenAt, String id) {
        LunchComment comment = new LunchComment();
        comment.id = UUID.fromString(id);
        comment.writtenAt = writtenAt;
        return comment;
    }
}
