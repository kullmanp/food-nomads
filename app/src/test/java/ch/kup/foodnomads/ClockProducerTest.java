package ch.kup.foodnomads;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;

class ClockProducerTest {

    @Test
    void producesTheSystemClockInUtc() {
        assertThat(new ClockProducer().clock()).isEqualTo(Clock.systemUTC());
    }
}
