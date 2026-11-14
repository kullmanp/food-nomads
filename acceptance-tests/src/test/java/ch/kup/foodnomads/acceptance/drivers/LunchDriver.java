package ch.kup.foodnomads.acceptance.drivers;

import java.time.LocalDate;
import java.util.Set;

/** Lunches, known to the tests by the id the app gave them. */
public interface LunchDriver {

    /** Plans a Lunch on this date as the logged-in Member, fails unless it was accepted, and returns its id. */
    String plan(LocalDate date);

    /** Fails unless planning a Lunch on this date is refused because the date is before today. */
    void assertPlanRejectedAsInThePast(LocalDate date);

    /** Fails unless planning a Lunch on this date is refused because a Lunch is planned already. */
    void assertPlanRejectedAsAlreadyPlanned(LocalDate date);

    /** Cancels whatever Lunch is planned, through the normal cancel. Does nothing without one. */
    void cancelPlannedLunch();

    /**
     * Fails unless the Lunch with this id is the Planned Lunch, on this date, with this Venue. A
     * null Venue means none yet.
     */
    void assertPlanned(String lunchId, LocalDate date, String venue);

    /** Cancels the Lunch with this id as the logged-in Member. */
    void cancel(String lunchId);

    /** Fails unless the Lunch with this id is a Cancelled Lunch, listed among the Lunches. */
    void assertCancelled(String lunchId);

    /** Comments on the Lunch with this id as the logged-in Member and fails unless it was accepted. */
    void comment(String lunchId, String text);

    /** Fails unless this comment on the Lunch with this id is refused because it has no text. */
    void assertCommentRejectedAsBlank(String lunchId, String text);

    /** Fails unless the Lunch with this id shows a comment by this Member's display name with this text. */
    void assertComment(String lunchId, String author, String text);

    /**
     * Records a Lunch from before the app on this date at the Venue with this name, as the
     * logged-in Member. Fails unless it was accepted, and returns its id.
     */
    String recordPast(LocalDate date, String venue);

    /** Fails unless recording a past Lunch at the Venue with this name is refused because it's visited. */
    void assertRecordPastRejectedAsVenueVisited(LocalDate date, String venue);

    /** Fails unless recording a past Lunch on this date is refused because the date isn't before today. */
    void assertRecordPastRejectedAsDateNotPast(LocalDate date, String venue);

    /** Fails unless the Lunch with this id is a Held Lunch, on this date, at the Venue with this name. */
    void assertHeld(String lunchId, LocalDate date, String venue);

    /** Puts up the Venue with this name as a Candidate Venue of the Lunch with this id, and fails unless it was accepted. */
    void addCandidate(String lunchId, String venue);

    /** Fails unless putting up the Venue with this name for the Lunch with this id is refused because it's visited. */
    void assertAddCandidateRejectedAsVenueVisited(String lunchId, String venue);

    /** Removes the Candidate Venue with this name from the Lunch with this id, and fails unless it was accepted. */
    void removeCandidate(String lunchId, String venue);

    /**
     * Fails unless the Lunch with this id lists exactly the Candidate Venues with these names, and
     * marks exactly those named in {@code visited} as visited.
     */
    void assertCandidates(String lunchId, Set<String> venues, Set<String> visited);

    /**
     * Votes {@code up} or {@code down} on the Candidate Venue with this name of the Lunch with this
     * id, as the logged-in Member, and fails unless it was accepted.
     */
    void vote(String lunchId, String venue, String direction);

    /** Withdraws the logged-in Member's Vote on the Candidate Venue with this name, and fails unless it was accepted. */
    void withdrawVote(String lunchId, String venue);

    /** Fails unless the Candidate Venue with this name of the Lunch with this id shows these numbers of Votes. */
    void assertVotes(String lunchId, String venue, int up, int down);

    /** Answers {@code in} or {@code out} for the Lunch with this id, as the logged-in Member, and fails unless it was accepted. */
    void rsvp(String lunchId, String answer);

    /** Fails unless answering for the Lunch with this id is refused because the Lunch isn't planned. */
    void assertRsvpRejectedAsNotPlanned(String lunchId, String answer);

    /**
     * Fails unless the Lunch with this id shows exactly the Members with these display names as in
     * and as out.
     */
    void assertRsvps(String lunchId, Set<String> in, Set<String> out);

    /** Moves the Lunch with this id to this date, as the logged-in Member, and fails unless it was accepted. */
    void move(String lunchId, LocalDate date);

    /** Fails unless moving the Lunch with this id to this date is refused because the date is before today. */
    void assertMoveRejectedAsInThePast(String lunchId, LocalDate date);

    /**
     * Fails unless this driver, with no Member logged in, is kept away from the Lunches: the UI
     * sends it to login, the API answers 401.
     */
    void assertNotAccessibleWithoutLogin();
}
