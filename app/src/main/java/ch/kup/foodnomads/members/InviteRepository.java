package ch.kup.foodnomads.members;

import java.util.Optional;

/** Where Invites are kept (ADR-0005). */
public interface InviteRepository {

    void add(Invite invite);

    Optional<Invite> find(String token);

    /** Like {@link #find}, but holds the Invite for this transaction, so that only one caller can redeem it. */
    Optional<Invite> findForUpdate(String token);
}
