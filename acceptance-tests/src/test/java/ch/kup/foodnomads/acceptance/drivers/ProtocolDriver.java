package ch.kup.foodnomads.acceptance.drivers;

import ch.kup.foodnomads.acceptance.drivers.api.ApiProtocolDriver;
import ch.kup.foodnomads.acceptance.drivers.ui.UiProtocolDriver;

/** One way of talking to the system under test: through the browser or through {@code /api}. */
public interface ProtocolDriver {

    SystemDriver system();

    /** The driver chosen with {@code -Ddriver=api|ui}, defaulting to {@code api}. */
    static ProtocolDriver selected() {
        String name = System.getProperty("driver", "api");
        return switch (name) {
            case "api" -> new ApiProtocolDriver();
            case "ui" -> new UiProtocolDriver();
            default -> throw new IllegalArgumentException(
                    "Unknown driver '" + name + "', expected -Ddriver=api or -Ddriver=ui");
        };
    }
}
