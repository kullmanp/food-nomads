package ch.kup.foodnomads.members;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Where Invites are kept (ADR-0005). */
public interface InviteRepository {

    void add(Invite invite);

    Optional<Invite> find(String token);

    /** Like {@link #find}, but holds the Invite for this transaction, so that only one caller can redeem it. */
    Optional<Invite> findForUpdate(String token);

    /** Like {@link #findForUpdate(String)}, so that revoking can't overwrite a redemption in progress. */
    Optional<Invite> findForUpdate(UUID id);

    /** All Invites, newest first, with their creating Member loaded. */
    List<Invite> newestFirst();
}
