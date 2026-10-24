package ch.kup.foodnomads.acceptance.drivers;

import ch.kup.foodnomads.acceptance.drivers.api.ApiProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.ui.UiProtocolDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;
import java.net.URI;

/**
 * One way of talking to the system under test: through the browser or through {@code /api}.
 * Each test gets its own instance and closes it afterwards.
 */
public interface ProtocolDriver extends AutoCloseable {

    SystemDriver system();

    MemberDriver members();

    @Override
    void close();

    /** The driver chosen with {@code -Ddriver=api|ui}, defaulting to {@code api}. */
    static ProtocolDriver selected() {
        String name = System.getProperty("driver", "api");
        return switch (name) {
            case "api" -> new ApiProtocolDriver(baseUrl());
            case "ui" -> new UiProtocolDriver(baseUrl());
            default -> throw new IllegalArgumentException(
                    "Unknown driver '" + name + "', expected -Ddriver=api or -Ddriver=ui");
        };
    }

    private static URI baseUrl() {
        return SystemUnderTest.get().baseUrl();
    }
}
