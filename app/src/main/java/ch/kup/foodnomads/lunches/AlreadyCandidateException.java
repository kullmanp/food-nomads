package ch.kup.foodnomads.lunches;

/** The Venue is a Candidate Venue of this Lunch already. */
public class AlreadyCandidateException extends RuntimeException {

    public AlreadyCandidateException() {
        super("Already a Candidate Venue");
    }
}
