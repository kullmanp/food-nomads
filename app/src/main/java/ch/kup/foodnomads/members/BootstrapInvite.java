package ch.kup.foodnomads.members;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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
    private final MemberRepository members;
    private final String baseUrl;
    private final Supplier<String> newToken;

    @Inject
    public BootstrapInvite(MemberRepository members, @ConfigProperty(name = "foodnomads.base-url") String baseUrl) {
        this(members, baseUrl, BootstrapInvite::randomToken);
    }

    BootstrapInvite(MemberRepository members, String baseUrl, Supplier<String> newToken) {
        this.members = members;
        this.baseUrl = baseUrl;
        this.newToken = newToken;
    }

    @Transactional
    void createIfNoMembers(@Observes StartupEvent event) {
        if (members.count() > 0) {
            return;
        }
        token.set(newToken.get());
        LOG.infof("Bootstrap Invite: %s", joinUrl().orElseThrow());
    }

    /** Where to redeem the Invite, if there is one. */
    Optional<String> joinUrl() {
        return Optional.ofNullable(token.get()).map(t -> baseUrl.replaceAll("/+$", "") + "/join/" + t);
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

    static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean matches(String current, String candidate) {
        return current != null && candidate != null
                && MessageDigest.isEqual(
                        current.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }
}
