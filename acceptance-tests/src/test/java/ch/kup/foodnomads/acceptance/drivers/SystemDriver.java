package ch.kup.foodnomads.acceptance.drivers;

public interface SystemDriver {

    /** Fails unless the system under test answers on this driver's channel. */
    void assertAvailable();
}
