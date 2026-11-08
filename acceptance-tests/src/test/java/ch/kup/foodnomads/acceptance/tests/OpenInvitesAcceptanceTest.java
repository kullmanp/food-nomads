package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class OpenInvitesAcceptanceTest extends AcceptanceTest {

    @Test
    void createdInviteIsListedWithItsLink() {
        members.logIn("member: founder");

        invites.create("invite: forAnna");

        invites.assertOpen("invite: forAnna", "createdBy: founder");
    }

    @Test
    void revokedInviteIsNoLongerOpenAndCannotBeRedeemed() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");

        invites.revoke("invite: forAnna");

        invites.assertNotOpen("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna", "expect: rejected");
    }

    @Test
    void anyMemberCanRevokeAnInvite() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        invites.create("invite: forBen");
        members.logOut();
        members.join("invite: forBen", "displayName: Ben");

        invites.revoke("invite: forAnna");

        invites.assertNotOpen("invite: forAnna");
    }

    @Test
    void redeemedInviteIsNoLongerOpen() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        invites.assertNotOpen("invite: forAnna");
    }
}
