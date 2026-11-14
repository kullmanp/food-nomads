package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class RsvpAcceptanceTest extends AcceptanceTest {

    @Test
    void membersAnswerInOrOut() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.rsvp("lunch: friday", "answer: out");

        lunches.assertRsvps("lunch: friday", "in: founder", "out: Anna");
    }

    @Test
    void memberChangesTheirAnswer() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");

        lunches.rsvp("lunch: friday", "answer: out");

        lunches.assertRsvps("lunch: friday", "in: ", "out: founder");
    }

    @Test
    void movingTheLunchClearsTheAnswersButKeepsTheChat() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");
        lunches.comment("lunch: friday", "text: Friday is bad for me after all");

        lunches.move("lunch: friday", "date: +8");

        lunches.assertPlanned("lunch: friday", "date: +8", "venue: none");
        lunches.assertRsvps("lunch: friday", "in: ", "out: ");
        lunches.assertComment("lunch: friday", "by: founder", "text: Friday is bad for me after all");
    }

    @Test
    void lunchCannotBeMovedIntoThePast() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.move("lunch: friday", "date: -1", "expect: dateInPast");
    }

    @Test
    void cancelledLunchTakesNoAnswers() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.cancel("lunch: friday");

        lunches.rsvp("lunch: friday", "answer: in", "expect: notPlanned");
    }
}
