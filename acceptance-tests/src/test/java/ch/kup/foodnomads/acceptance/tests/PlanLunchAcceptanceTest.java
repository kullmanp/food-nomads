package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class PlanLunchAcceptanceTest extends AcceptanceTest {

    @Test
    void plannedLunchHasNoVenueYet() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: friday", "date: +7");

        lunches.assertPlanned("lunch: friday", "date: +7", "venue: none");
    }

    @Test
    void lunchCanBePlannedForToday() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: now", "date: today");

        lunches.assertPlanned("lunch: now", "date: today", "venue: none");
    }

    @Test
    void lunchCannotBePlannedInThePast() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: yesterday", "date: -1", "expect: dateInPast");
    }

    @Test
    void onlyOneLunchIsPlannedAtATime() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.plan("lunch: later", "date: +14", "expect: alreadyPlanned");

        lunches.assertPlanned("lunch: friday", "date: +7", "venue: none");
    }

    @Test
    void cancelledLunchMakesRoomForTheNextOne() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.cancel("lunch: friday");

        lunches.assertCancelled("lunch: friday");
        lunches.plan("lunch: later", "date: +14");
        lunches.assertPlanned("lunch: later", "date: +14", "venue: none");
    }

    @Test
    void membersTalkALunchOver() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        lunches.plan("lunch: friday", "date: +7");
        lunches.comment("lunch: friday", "text: Only if we stay near Barfi");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.comment("lunch: friday", "text: Barfi suits me");

        lunches.assertComment("lunch: friday", "by: founder", "text: Only if we stay near Barfi");
        lunches.assertComment("lunch: friday", "by: Anna", "text: Barfi suits me");
    }

    @Test
    void commentNeedsText() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.comment("lunch: friday", "text: ", "expect: textRequired");
    }

    @Test
    void lunchesAreNotAccessibleWithoutLogin() {
        lunches.assertNotAccessibleWithoutLogin();
    }
}
