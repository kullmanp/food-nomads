package ch.kup.foodnomads.lunches;

/** There is no Lunch with this id. */
public class LunchNotFoundException extends RuntimeException {

    public LunchNotFoundException() {
        super("Lunch not found");
    }
}
