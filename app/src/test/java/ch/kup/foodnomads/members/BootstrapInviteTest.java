package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.member;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class BootstrapInviteTest {

    private final InMemoryMemberRepository members = new InMemoryMemberRepository();
    private final BootstrapInvite invite = new BootstrapInvite(members, "https://nomads.example/", () -> "secret");

    @Test
    void existsOnceStartedWithNoMembers() {
        invite.createIfNoMembers(null);

        assertThat(invite.isValid("secret")).isTrue();
    }

    @Test
    void doesNotExistBeforeStartup() {
        assertThat(invite.isValid("secret")).isFalse();
    }

    @Test
    void doesNotExistOnceThereAreMembers() {
        members.add(member("Anna"));

        invite.createIfNoMembers(null);

        assertThat(invite.isValid("secret")).isFalse();
    }

    @Test
    void rejectsAnyOtherToken() {
        invite.createIfNoMembers(null);

        assertThat(invite.isValid("other")).isFalse();
        assertThat(invite.isValid("secre")).isFalse();
        assertThat(invite.isValid(null)).isFalse();
    }

    @Test
    void canBeRedeemedOnlyOnce() {
        invite.createIfNoMembers(null);

        invite.consume("secret");

        assertThat(invite.isValid("secret")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invite.consume("secret"));
    }

    @Test
    void isNotUsedUpByAWrongToken() {
        invite.createIfNoMembers(null);

        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invite.consume("other"));
        assertThat(invite.isValid("secret")).isTrue();
    }

    @Test
    void cannotBeRedeemedBeforeItExists() {
        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> invite.consume("secret"));
    }

    @Test
    void hasAJoinUrlOnTheBaseUrl() {
        invite.createIfNoMembers(null);

        assertThat(invite.joinUrl()).contains("https://nomads.example/join/secret");
    }

    @Test
    void hasNoJoinUrlBeforeItExists() {
        assertThat(invite.joinUrl()).isEmpty();
    }

    @Test
    void usesAnUnguessableUrlSafeToken() {
        String token = BootstrapInvite.randomToken();

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(BootstrapInvite.randomToken()).isNotEqualTo(token);
    }
}
