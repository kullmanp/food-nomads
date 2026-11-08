package ch.kup.foodnomads.members;

/** The caller is logged in as a Member, so they can't join again. */
public class AlreadyMemberException extends RuntimeException {

    public AlreadyMemberException() {
        super("Already a Member");
    }
}
