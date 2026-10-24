package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import java.net.URI;

class ApiMemberDriver implements MemberDriver {

    @Override
    public void assertBootstrapInviteRejected(URI joinUrl) {
        throw new UnsupportedOperationException(
                "Not implemented yet: WebAuthn registration with the Bootstrap Invite token is refused");
    }
}
