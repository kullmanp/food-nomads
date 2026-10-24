package ch.kup.foodnomads.api;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/**
 * Answers 401 when an {@code /api} request needs a logged-in Member, instead of the WebAuthn
 * mechanism's redirect to the login page. It authenticates nobody itself, and for pages it
 * sends no challenge, so the WebAuthn mechanism's redirect applies there.
 */
@ApplicationScoped
public class ApiAuthenticationChallenge implements HttpAuthenticationMechanism {

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (!context.normalizedPath().startsWith("/api/")) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(new ChallengeData(401, null, null));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().nullItem();
    }

    /** Ahead of the WebAuthn mechanism, so its challenge is asked first. */
    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY + 1;
    }
}
