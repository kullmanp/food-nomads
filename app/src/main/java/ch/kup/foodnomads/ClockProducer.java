package ch.kup.foodnomads;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Clock;

/** The {@link Clock} the application layer tells time with, so unit tests can use their own. */
@ApplicationScoped
class ClockProducer {

    @Produces
    @Singleton
    Clock clock() {
        return Clock.systemUTC();
    }
}
