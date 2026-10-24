package ch.kup.foodnomads.members;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * The single-use Invite that lets the first Member join. It exists only while there are no
 * Members: it is created at startup and logged, and redeeming it consumes it.
 */
@ApplicationScoped
public class BootstrapInvite {

    private static final Logger LOG = Logger.getLogger(BootstrapInvite.class);

    private final AtomicReference<String> token = new AtomicReference<>();

    @ConfigProperty(name = "foodnomads.base-url")
    String baseUrl;

    @Transactional
    void createIfNoMembers(@Observes StartupEvent event) {
        if (Member.count() > 0) {
            return;
        }
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        token.set(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        LOG.infof("Bootstrap Invite: %s/join/%s", baseUrl.replaceAll("/+$", ""), token.get());
    }

    public boolean isValid(String candidate) {
        return matches(token.get(), candidate);
    }

    /** Uses up the Invite, so it can be redeemed only once. */
    void consume(String candidate) {
        String current = token.get();
        if (!matches(current, candidate) || !token.compareAndSet(current, null)) {
            throw new InviteRejectedException();
        }
    }

    private static boolean matches(String current, String candidate) {
        return current != null && candidate != null
                && MessageDigest.isEqual(
                        current.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }
}
