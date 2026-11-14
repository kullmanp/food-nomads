package ch.kup.foodnomads.lunches;

/** The date is before today in Basel. */
public class DateInPastException extends RuntimeException {

    public DateInPastException() {
        super("Date in past");
    }
}
