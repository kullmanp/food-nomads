package ch.kup.foodnomads.lunches;

import static org.assertj.core.api.Assertions.assertThat;

import ch.kup.foodnomads.members.Member;
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

    @Test
    void listsWhoIsInAndWhoIsOutByName() {
        Lunch lunch = new Lunch();
        Member ben = member("Ben");
        Member anna = member("Anna");
        Member oskar = member("Oskar");
        Member aenne = member("Änne");
        lunch.rsvps = new ArrayList<>(List.of(
                rsvp(ben, RsvpAnswer.IN),
                rsvp(oskar, RsvpAnswer.OUT),
                rsvp(aenne, RsvpAnswer.IN),
                rsvp(anna, RsvpAnswer.IN)));

        // As a German speaker sorts them: Änne between Anna and Ben, not after Oskar.
        assertThat(lunch.answered(RsvpAnswer.IN)).containsExactly(anna, aenne, ben);
        assertThat(lunch.answered(RsvpAnswer.OUT)).containsExactly(oskar);
    }

    @Test
    void countsWhoIsIn() {
        Lunch lunch = new Lunch();
        assertThat(lunch.inCount()).isZero();

        lunch.rsvps = new ArrayList<>(List.of(
                rsvp(member("Anna"), RsvpAnswer.IN),
                rsvp(member("Ben"), RsvpAnswer.OUT),
                rsvp(member("Cleo"), RsvpAnswer.IN)));

        assertThat(lunch.inCount()).isEqualTo(2);
    }

    @Test
    void findsAMembersAnswerByTheirId() {
        Lunch lunch = new Lunch();
        Member anna = member("Anna");
        Member ben = member("Ben");
        lunch.rsvps = new ArrayList<>(List.of(rsvp(anna, RsvpAnswer.OUT)));
        Member annaAgain = new Member();
        annaAgain.id = anna.id;

        assertThat(lunch.answerOf(annaAgain)).isEqualTo(RsvpAnswer.OUT);
        assertThat(lunch.answerOf(ben)).isNull();
    }

    private static Rsvp rsvp(Member member, RsvpAnswer answer) {
        Rsvp rsvp = new Rsvp();
        rsvp.id = UUID.randomUUID();
        rsvp.member = member;
        rsvp.answer = answer;
        return rsvp;
    }

    private static Member member(String displayName) {
        Member member = new Member();
        member.id = UUID.randomUUID();
        member.displayName = displayName;
        return member;
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
