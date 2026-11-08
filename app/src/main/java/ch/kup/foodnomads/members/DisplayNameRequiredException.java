package ch.kup.foodnomads.members;

/** A new Member needs a display name, and this one is blank. */
public class DisplayNameRequiredException extends IllegalArgumentException {

    public DisplayNameRequiredException() {
        super("A display name is required");
    }
}
