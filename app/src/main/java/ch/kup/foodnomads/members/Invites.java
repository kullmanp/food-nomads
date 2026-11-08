package ch.kup.foodnomads.members;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Creating Invites and redeeming them. */
@ApplicationScoped
public class Invites {

    private final InviteRepository repository;
    private final Clock clock;
    private final String baseUrl;
    private final Supplier<String> newToken;

    @Inject
    public Invites(
            InviteRepository repository, Clock clock, @ConfigProperty(name = "foodnomads.base-url") String baseUrl) {
        this(repository, clock, baseUrl, BootstrapInvite::randomToken);
    }

    Invites(InviteRepository repository, Clock clock, String baseUrl, Supplier<String> newToken) {
        this.repository = repository;
        this.clock = clock;
        this.baseUrl = baseUrl;
        this.newToken = newToken;
    }

    @Transactional
    public Invite create(Member creator) {
        Invite invite = new Invite();
        invite.id = UUID.randomUUID();
        invite.token = newToken.get();
        invite.createdBy = creator;
        invite.createdAt = clock.instant();
        repository.add(invite);
        return invite;
    }

    /** Where to redeem this Invite. */
    public String joinUrl(Invite invite) {
        return BootstrapInvite.joinUrl(baseUrl, invite.token);
    }

    @Transactional
    public boolean isOpen(String token) {
        Instant now = clock.instant();
        return repository.find(token).filter(invite -> invite.isOpenAt(now)).isPresent();
    }

    /**
     * Uses up the Invite for this Member, or fails with {@link InviteRejectedException}. The row
     * stays locked until the transaction ends, so two people can't both redeem one link, and a
     * join that fails afterwards leaves it valid.
     */
    @Transactional
    void redeem(String token, Member member) {
        Instant now = clock.instant();
        Invite invite =
                repository.findForUpdate(token).filter(i -> i.isOpenAt(now)).orElseThrow(InviteRejectedException::new);
        invite.redeemedBy = member;
        invite.redeemedAt = now;
    }
}
