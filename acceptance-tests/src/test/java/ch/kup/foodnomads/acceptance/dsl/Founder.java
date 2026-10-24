package ch.kup.foodnomads.acceptance.dsl;

import ch.kup.foodnomads.acceptance.drivers.MemberDriver;
import ch.kup.foodnomads.acceptance.drivers.ProtocolDriver;
import ch.kup.foodnomads.acceptance.sut.SystemUnderTest;

/**
 * The first Member, who joins through the Bootstrap Invite once per run on the active driver.
 * The Bootstrap Invite is global state, so tests share {@code founder} instead of each joining.
 * If joining fails, every test fails with the cause.
 */
final class Founder {

    static final String ALIAS = "founder";

    private Founder() {
    }

    static void ensureJoined() {
        if (Holder.FAILURE != null) {
            throw new IllegalStateException(
                    "The founder could not join through the Bootstrap Invite, so no test can run", Holder.FAILURE);
        }
    }

    private static final class Holder {
        static final Throwable FAILURE = join();
    }

    private static Throwable join() {
        try (ProtocolDriver driver = ProtocolDriver.selected()) {
            MemberDriver members = driver.members();
            members.join(SystemUnderTest.get().bootstrapInviteUrl(), ALIAS);
            members.assertLoggedIn(ALIAS);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
