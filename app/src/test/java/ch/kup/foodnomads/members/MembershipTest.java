package ch.kup.foodnomads.members;

import static ch.kup.foodnomads.members.TestData.credentialId;
import static ch.kup.foodnomads.members.TestData.member;
import static ch.kup.foodnomads.members.TestData.registration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MembershipTest {

    private final InMemoryMemberRepository members = new InMemoryMemberRepository();
    private final InMemoryPasskeyRepository passkeys = new InMemoryPasskeyRepository();
    private final BootstrapInvite invite = new BootstrapInvite(members, "https://nomads.example", () -> "secret");
    private final MutableClock clock = new MutableClock(Instant.parse("2026-03-01T10:00:00Z"));
    private final InMemoryInviteRepository inviteRepository = new InMemoryInviteRepository();
    private final Invites invites = new Invites(inviteRepository, clock, "https://nomads.example", () -> "stored");
    private final Membership membership = new Membership(invite, invites, members, passkeys);

    @BeforeEach
    void startUp() {
        invite.createIfNoMembers(null);
    }

    @Test
    void acceptsTheBootstrapInvite() {
        assertThat(membership.isInviteValid("secret")).isTrue();
        membership.checkInvite("secret");
    }

    @Test
    void rejectsAnUnknownInvite() {
        assertThat(membership.isInviteValid("other")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> membership.checkInvite("other"));
    }

    @Test
    void aVisitorCanJoinWithAValidInvite() {
        assertThatCode(() -> membership.checkCanJoin(anonymous(), "secret")).doesNotThrowAnyException();
    }

    @Test
    void aVisitorCannotJoinWithAnInvalidInvite() {
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> membership.checkCanJoin(anonymous(), "other"));
    }

    @Test
    void aLoggedInMemberCannotJoinAgainAndTheInviteStaysValid() {
        invites.create(member("Founder"));
        Member anna = member("Anna");
        members.add(anna);

        assertThatExceptionOfType(AlreadyMemberException.class)
                .isThrownBy(() -> membership.checkCanJoin(loggedInAs(anna.id.toString()), "stored"));

        assertThat(membership.isInviteValid("stored")).isTrue();
    }

    @Test
    void aLoggedInMemberHearsTheyAreAMemberEvenForAnInvalidInvite() {
        Member anna = member("Anna");
        members.add(anna);

        assertThatExceptionOfType(AlreadyMemberException.class)
                .isThrownBy(() -> membership.checkCanJoin(loggedInAs(anna.id.toString()), "other"));
    }

    @Test
    void aSessionWithoutAMemberIsTreatedLikeAVisitor() {
        String nobody = member("Gone").id.toString();

        assertThatCode(() -> membership.checkCanJoin(loggedInAs(nobody), "secret"))
                .doesNotThrowAnyException();
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> membership.checkCanJoin(loggedInAs(nobody), "other"));
    }

    @Test
    void joiningCreatesAMemberWithTheirPasskey() {
        Instant before = Instant.now();

        Member member = membership.join("secret", "  Anna \t", registration(credentialId("cred-1")));

        assertThat(member.id).isNotNull();
        assertThat(member.displayName).isEqualTo("Anna");
        assertThat(member.joinedAt).isBetween(before, Instant.now());
        assertThat(members.find(member.id)).containsSame(member);
        assertThat(passkeys.find(credentialId("cred-1")))
                .hasValueSatisfying(p -> assertThat(p.member).isSameAs(member));
    }

    @Test
    void joiningRedeemsTheInvite() {
        membership.join("secret", "Anna", registration(credentialId("cred-1")));

        assertThat(membership.isInviteValid("secret")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> membership.join("secret", "Ben", registration(credentialId("cred-2"))));
        assertThat(members.count()).isEqualTo(1);
    }

    @Test
    void joiningNeedsAValidInvite() {
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> membership.join("other", "Anna", registration(credentialId("cred-1"))));
        assertThat(members.count()).isZero();
        assertThat(passkeys.find(credentialId("cred-1"))).isEmpty();
    }

    @Test
    void joiningNeedsADisplayName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> membership.join("secret", null, registration(credentialId("cred-1"))));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> membership.join("secret", " \t", registration(credentialId("cred-1"))));
        assertThat(members.count()).isZero();
        assertThat(membership.isInviteValid("secret"))
                .as("a failed join keeps the Invite")
                .isTrue();
    }

    @Test
    void aFreeDisplayNameIsAccepted() {
        members.add(member("Anna"));

        assertThat(membership.checkDisplayName("  Ben \t")).isEqualTo("Ben");
    }

    @Test
    void aBlankDisplayNameIsRequired() {
        assertThatExceptionOfType(DisplayNameRequiredException.class)
                .isThrownBy(() -> membership.checkDisplayName(null));
        assertThatExceptionOfType(DisplayNameRequiredException.class)
                .isThrownBy(() -> membership.checkDisplayName(" \t"));
    }

    @Test
    void aDisplayNameIsTakenIgnoringCase() {
        members.add(member("Anna"));

        assertThatExceptionOfType(DisplayNameTakenException.class)
                .isThrownBy(() -> membership.checkDisplayName("Anna"));
        assertThatExceptionOfType(DisplayNameTakenException.class)
                .isThrownBy(() -> membership.checkDisplayName("anna"));
        assertThatExceptionOfType(DisplayNameTakenException.class)
                .isThrownBy(() -> membership.checkDisplayName("ANNA"));
    }

    @Test
    void aDisplayNameIsTakenIgnoringCaseAndSurroundingSpace() {
        members.add(member("Jürg"));

        assertThatExceptionOfType(DisplayNameTakenException.class)
                .isThrownBy(() -> membership.checkDisplayName("  JÜRG "));
    }

    @Test
    void joiningRefusesATakenDisplayNameAndLeavesTheInviteValid() {
        invites.create(member("Founder"));
        members.add(member("Anna"));

        assertThatExceptionOfType(DisplayNameTakenException.class)
                .isThrownBy(() -> membership.join("stored", "ANNA", registration(credentialId("cred-1"))));

        assertThat(members.count()).isEqualTo(1);
        assertThat(passkeys.find(credentialId("cred-1"))).isEmpty();
        assertThat(membership.isInviteValid("stored")).isTrue();
    }

    @Test
    void joiningNeedsADisplayNameEvenThroughAStoredInvite() {
        invites.create(member("Founder"));

        assertThatExceptionOfType(DisplayNameRequiredException.class)
                .isThrownBy(() -> membership.join("stored", "  ", registration(credentialId("cred-1"))));

        assertThat(membership.isInviteValid("stored")).isTrue();
    }

    @Test
    void acceptsAStoredInvite() {
        invites.create(member("Founder"));

        assertThat(membership.isInviteValid("stored")).isTrue();
        membership.checkInvite("stored");
    }

    @Test
    void rejectsAnExpiredInvite() {
        invites.create(member("Founder"));
        clock.advance(Duration.ofDays(7));

        assertThat(membership.isInviteValid("stored")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class).isThrownBy(() -> membership.checkInvite("stored"));
    }

    @Test
    void joiningThroughAStoredInviteRecordsTheNewMember() {
        Invite stored = invites.create(member("Founder"));

        Member anna = membership.join("stored", "Anna", registration(credentialId("cred-1")));

        assertThat(members.find(anna.id)).containsSame(anna);
        assertThat(passkeys.find(credentialId("cred-1")))
                .hasValueSatisfying(p -> assertThat(p.member).isSameAs(anna));
        assertThat(stored.redeemedBy).isSameAs(anna);
        assertThat(stored.redeemedAt).isEqualTo(clock.instant());
    }

    @Test
    void joiningThroughAStoredInviteLeavesTheBootstrapInviteAlone() {
        invites.create(member("Founder"));

        membership.join("stored", "Anna", registration(credentialId("cred-1")));

        assertThat(membership.isInviteValid("secret")).isTrue();
    }

    @Test
    void joiningThroughTheBootstrapInviteLeavesStoredInvitesAlone() {
        invites.create(member("Founder"));

        membership.join("secret", "Anna", registration(credentialId("cred-1")));

        assertThat(membership.isInviteValid("stored")).isTrue();
    }

    @Test
    void aStoredInviteCanBeRedeemedOnlyOnce() {
        invites.create(member("Founder"));
        membership.join("stored", "Anna", registration(credentialId("cred-1")));

        assertThat(membership.isInviteValid("stored")).isFalse();
        assertThatExceptionOfType(InviteRejectedException.class)
                .isThrownBy(() -> membership.join("stored", "Ben", registration(credentialId("cred-2"))));
        assertThat(members.count()).isEqualTo(1);
        assertThat(passkeys.find(credentialId("cred-2"))).isEmpty();
    }

    @Test
    void aFailedJoinLeavesTheStoredInviteValid() {
        invites.create(member("Founder"));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> membership.join("stored", " ", registration(credentialId("cred-1"))));

        assertThat(membership.isInviteValid("stored")).isTrue();
        assertThat(members.count()).isZero();
    }

    @Test
    void findsAMemberByTheirId() {
        Member anna = member("Anna");
        members.add(anna);

        assertThat(membership.find(anna.id.toString())).containsSame(anna);
    }

    @Test
    void findsNobodyForAnUnknownOrMalformedId() {
        members.add(member("Anna"));

        assertThat(membership.find(member("Ben").id.toString())).isEmpty();
        assertThat(membership.find("not-a-uuid")).isEmpty();
        assertThat(membership.find(null)).isEmpty();
    }

    @Test
    void theCurrentMemberIsTheLoggedInPrincipal() {
        Member anna = member("Anna");
        members.add(anna);

        assertThat(membership.current(loggedInAs(anna.id.toString()))).containsSame(anna);
    }

    @Test
    void thereIsNoCurrentMemberWhenAnonymous() {
        assertThat(membership.current(anonymous())).isEmpty();
    }

    private static SecurityIdentity anonymous() {
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(""))
                .setAnonymous(true)
                .build();
    }

    private static SecurityIdentity loggedInAs(String principal) {
        return QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(principal))
                .build();
    }
}
