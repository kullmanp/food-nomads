package ch.kup.foodnomads.members;

/** The Invite is unknown, already used, or no longer valid. */
public class InviteRejectedException extends RuntimeException {

    public InviteRejectedException() {
        super("Invite rejected");
    }
}
