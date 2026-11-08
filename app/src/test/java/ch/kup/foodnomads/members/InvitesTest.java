package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InvitesTest {

    private static final Instant CREATED = Instant.parse("2026-03-01T10:00:00Z");
    private static final Duration SEVEN_DAYS = Duration.ofDays(7);
    private static final Duration JUST_BEFORE_SEVEN_DAYS = SEVEN_DAYS.minusNanos(1);

    private final MutableClock clock = new MutableClock(CREATED);
    private final InMemoryInviteRepository repository = new InMemoryInviteRepository();
    private final Invites invites = new Invites(repository, clock, "https://nomads.example/", () -> "secret");
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
}
