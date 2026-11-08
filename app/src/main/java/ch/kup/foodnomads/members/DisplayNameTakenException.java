package ch.kup.foodnomads.members;

/** Another Member already has this display name, ignoring case. */
public class DisplayNameTakenException extends RuntimeException {

    public DisplayNameTakenException() {
        super("Display name taken");
    }
}
