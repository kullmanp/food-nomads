package ch.kup.foodnomads.acceptance.tests;

import ch.kup.foodnomads.acceptance.dsl.AcceptanceTest;
import org.junit.jupiter.api.Test;

class BootstrapInviteAcceptanceTest extends AcceptanceTest {

    @Test
    void bootstrapInviteCannotBeUsedOnceAMemberExists() {
        members.assertBootstrapInviteRejected();
    }
}
