package ch.kup.foodnomads.lunches;

/** This Candidate Venue is the Lunch's Venue, so it can't be removed. Clear the pick first. */
public class CandidatePickedException extends RuntimeException {

    public CandidatePickedException() {
        super("Candidate Venue picked");
    }
}
