package ch.kup.foodnomads.acceptance.sut;

import java.net.URI;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The packaged app plus PostgreSQL, started once per test run and shared by all tests.
 * Tests never reset it; the DSL's aliasing keeps them apart.
 */
public final class SystemUnderTest {

    private static final String POSTGRES_IMAGE = "postgres:18-alpine";
    private static final int APP_PORT = 8080;

    private final GenericContainer<?> app;

    private SystemUnderTest(GenericContainer<?> app) {
        this.app = app;
    }

    public static SystemUnderTest get() {
        return Holder.INSTANCE;
    }

    public URI baseUrl() {
        return URI.create("http://" + app.getHost() + ":" + app.getMappedPort(APP_PORT) + "/");
    }

    /** The join URL of the Bootstrap Invite, as the app logged it at startup. */
    public URI bootstrapInviteUrl() {
        throw new UnsupportedOperationException(
                "Not implemented yet: the app logs the Bootstrap Invite join URL at startup");
    }

    private static final class Holder {
        static final SystemUnderTest INSTANCE = start();
    }

    private static SystemUnderTest start() {
        String appImage = System.getProperty("app.image");
        if (appImage == null || appImage.isBlank()) {
            throw new IllegalStateException(
                    "System property app.image is not set. Run the suite through Maven (./mvnw verify).");
        }

        Network network = Network.newNetwork();

        PostgreSQLContainer postgres = new PostgreSQLContainer(POSTGRES_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("postgres");
        postgres.start();

        GenericContainer<?> app = new GenericContainer<>(DockerImageName.parse(appImage))
                .withNetwork(network)
                .withEnv("QUARKUS_DATASOURCE_JDBC_URL",
                        "jdbc:postgresql://postgres:5432/" + postgres.getDatabaseName())
                .withEnv("QUARKUS_DATASOURCE_USERNAME", postgres.getUsername())
                .withEnv("QUARKUS_DATASOURCE_PASSWORD", postgres.getPassword())
                .withExposedPorts(APP_PORT)
                .waitingFor(Wait.forHttp("/q/health/ready")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
        app.start();

        return new SystemUnderTest(app);
    }
}
