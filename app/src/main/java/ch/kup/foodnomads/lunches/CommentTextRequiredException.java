package ch.kup.foodnomads.lunches;

/** A Lunch Comment needs some text. */
public class CommentTextRequiredException extends RuntimeException {

    public CommentTextRequiredException() {
        super("Comment text required");
    }
}
