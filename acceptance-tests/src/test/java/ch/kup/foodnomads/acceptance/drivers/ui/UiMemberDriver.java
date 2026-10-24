package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import java.net.URI;

class UiMemberDriver implements MemberDriver {

    @Override
    public void assertBootstrapInviteRejected(URI joinUrl) {
        throw new UnsupportedOperationException(
                "Not implemented yet: the join page refuses the Bootstrap Invite");
    }
}
