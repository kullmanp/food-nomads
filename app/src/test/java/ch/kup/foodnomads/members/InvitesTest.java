package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class InvitesTest {

    private static final Instant CREATED = Instant.parse("2026-03-01T10:00:00Z");
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);
    private static final Duration JUST_BEFORE_SEVEN_DAYS = SEVEN_DAYS.minusNanos(1);

    private final MutableClock clock = new MutableClock(CREATED);
    private final InMemoryInviteRepository repository = new InMemoryInviteRepository();
    private final Invites invites = new Invites(repository, clock, "https://nomads.example/", () -> "secret");
    private final Iterator<String> tokens =
            Stream.iterate(1, i -> i + 1).map(i -> "token" + i).iterator();
    /** For tests that need several Invites at once, which then can't share a token. */
    private final Invites severalInvites = new Invites(repository, clock, "https://nomads.example/", tokens::next);

    private final Member anna = member("Anna");
    private final Member ben = member("Ben");

    @Test
    void creatingStoresAnInviteWithItsCreatorAndTime() {
        Invite invite = invites.create(anna);

        assertThat(invite.id).isNotNull();
        assertThat(invite.token).isEqualTo("secret");
        assertThat(invite.createdBy).isSameAs(anna);
        assertThat(invite.createdAt).isEqualTo(CREATED);
        assertThat(invite.redeemedBy).isNull();
        assertThat(invite.redeemedAt).isNull();
        assertThat(repository.find("secret")).containsSame(invite);
    }

    @Test
    void everyInviteGetsItsOwnId() {
        assertThat(invites.create(anna).id).isNotEqualTo(invites.create(anna).id);
    }

    @Test
    void hasAJoinUrlOnTheBaseUrl() {
        assertThat(invites.joinUrl(invites.create(anna))).isEqualTo("https://nomads.example/join/secret");
    }

    @Test
    void expiresSevenDaysAfterItWasCreated() {
        assertThat(invites.create(anna).expiresAt()).isEqualTo(CREATED.plus(SEVEN_DAYS));
    }

    @Test
    void isOpenOnceCreated() {
        invites.create(anna);

        assertThat(invites.isOpen("secret")).isTrue();
    }

    @Test
    void isNotOpenForAnUnknownToken() {
        invites.create(anna);

        assertThat(invites.isOpen("other")).isFalse();
    }

    @Test
    void isStillOpenJustBeforeSevenDays() {
        invites.create(anna);

        clock.advance(JUST_BEFORE_SEVEN_DAYS);

        assertThat(invites.isOpen("secret")).isTrue();
    }

    @Test
    void isExpiredAtExactlySevenDays() {
        invites.create(anna);

        clock.advance(SEVEN_DAYS);

        assertThat(invites.isOpen("secret")).isFalse();
    }

    @Test
    void isExpiredAfterSevenDays() {
        invites.create(anna);

        clock.advance(SEVEN_DAYS.plusNanos(1));

        assertThat(invites.isOpen("secret")).isFalse();
    }

    @Test
    void redeemingRecordsWhoAndWhen() {
        Invite invite = invites.create(anna);
        clock.advance(Duration.ofHours(1));

        invites.redeem("secret", ben);

        assertThat(invite.redeemedBy).isSameAs(ben);
        assertThat(invite.redeemedAt).isEqualTo(CREATED.plus(Duration.ofHours(1)));
    }

    @Test
    void canBeRedeemedOnlyOnce() {
        Invite invite = invites.create(anna);
        invites.redeem("secret", ben);

        assertThat(invites.isOpen("secret")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> invites.redeem("secret", member("Clara")));
        assertThat(invite.redeemedBy).isSameAs(ben);
    }

    @Test
    void canBeRedeemedJustBeforeSevenDays() {
        Invite invite = invites.create(anna);
        clock.advance(JUST_BEFORE_SEVEN_DAYS);

        invites.redeem("secret", ben);

        assertThat(invite.redeemedBy).isSameAs(ben);
    }

    @Test
    void cannotBeRedeemedAtExactlySevenDays() {
        Invite invite = invites.create(anna);
        clock.advance(SEVEN_DAYS);

        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invites.redeem("secret", ben));
        assertThat(invite.redeemedBy).isNull();
    }

    @Test
    void cannotRedeemAnUnknownToken() {
        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invites.redeem("other", ben));
    }

    @Test
    void revokingRecordsWhenAndKeepsTheInviteStored() {
        Invite invite = invites.create(anna);
        clock.advance(Duration.ofHours(1));

        invites.revoke(invite.id);

        assertThat(invite.revokedAt).isEqualTo(CREATED.plus(Duration.ofHours(1)));
        assertThat(repository.find("secret")).containsSame(invite);
    }

    @Test
    void isNotOpenOnceRevoked() {
        invites.revoke(invites.create(anna).id);

        assertThat(invites.isOpen("secret")).isFalse();
    }

    @Test
    void cannotBeRedeemedOnceRevoked() {
        Invite invite = invites.create(anna);
        invites.revoke(invite.id);

        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invites.redeem("secret", ben));
        assertThat(invite.redeemedBy).isNull();
    }

    @Test
    void canBeRevokedJustBeforeSevenDays() {
        Invite invite = invites.create(anna);
        clock.advance(JUST_BEFORE_SEVEN_DAYS);

        invites.revoke(invite.id);

        assertThat(invite.revokedAt).isEqualTo(clock.instant());
    }

    @Test
    void revokingAnExpiredInviteChangesNothing() {
        Invite invite = invites.create(anna);
        clock.advance(SEVEN_DAYS);

        invites.revoke(invite.id);

        assertThat(invite.revokedAt).isNull();
    }

    @Test
    void revokingARedeemedInviteChangesNothing() {
        Invite invite = invites.create(anna);
        invites.redeem("secret", ben);

        invites.revoke(invite.id);

        assertThat(invite.revokedAt).isNull();
        assertThat(invite.redeemedBy).isSameAs(ben);
    }

    @Test
    void revokingARevokedInviteChangesNothing() {
        Invite invite = invites.create(anna);
        invites.revoke(invite.id);
        Instant firstRevoked = invite.revokedAt;
        clock.advance(Duration.ofHours(1));

        invites.revoke(invite.id);

        assertThat(invite.revokedAt).isEqualTo(firstRevoked);
    }

    @Test
    void revokingAnUnknownInviteChangesNothing() {
        Invite invite = invites.create(anna);

        invites.revoke(UUID.randomUUID());

        assertThat(invite.revokedAt).isNull();
    }

    @Test
    void listsOpenInvitesNewestFirst() {
        Invite oldest = severalInvites.create(anna);
        clock.advance(Duration.ofHours(1));
        Invite newest = severalInvites.create(ben);

        assertThat(severalInvites.open()).containsExactly(newest, oldest);
    }

    @Test
    void listLeavesOutRedeemedInvites() {
        Invite redeemed = severalInvites.create(anna);
        Invite open = severalInvites.create(anna);
        severalInvites.redeem(redeemed.token, ben);

        assertThat(severalInvites.open()).containsExactly(open);
    }

    @Test
    void listLeavesOutRevokedInvites() {
        Invite revoked = severalInvites.create(anna);
        Invite open = severalInvites.create(anna);
        severalInvites.revoke(revoked.id);

        assertThat(severalInvites.open()).containsExactly(open);
    }

    @Test
    void listStillHasAnInviteJustBeforeSevenDays() {
        Invite invite = invites.create(anna);

        clock.advance(JUST_BEFORE_SEVEN_DAYS);

        assertThat(invites.open()).containsExactly(invite);
    }

    @Test
    void listLeavesOutAnInviteAtExactlySevenDays() {
        invites.create(anna);

        clock.advance(SEVEN_DAYS);

        assertThat(invites.open()).isEmpty();
    }
}
