package ch.kup.foodnomads.lunches;

/** The Venue isn't a Candidate Venue of this Lunch. */
public class NotACandidateException extends RuntimeException {

    public NotACandidateException() {
        super("Not a Candidate Venue");
    }
}
