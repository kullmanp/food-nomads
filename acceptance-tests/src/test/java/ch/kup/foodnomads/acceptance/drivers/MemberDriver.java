package ch.kup.foodnomads.acceptance.drivers;

import java.net.URI;

public interface MemberDriver {

    /** Fails unless registering a passkey through the given join URL is refused. */
    void assertBootstrapInviteRejected(URI joinUrl);
}
