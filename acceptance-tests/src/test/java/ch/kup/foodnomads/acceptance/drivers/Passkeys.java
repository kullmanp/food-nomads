package ch.kup.foodnomads.acceptance.drivers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The passkeys Members registered during this run, by Member. Each test starts with a fresh
 * browser context or HTTP client and imports the passkey of the Member it acts as.
 */
public final class Passkeys {

    private static final Map<String, Passkey> BY_MEMBER = new ConcurrentHashMap<>();

    private Passkeys() {
    }

    public static void store(String member, Passkey passkey) {
        BY_MEMBER.put(member, passkey);
    }

    public static Passkey of(String member) {
        Passkey passkey = BY_MEMBER.get(member);
        if (passkey == null) {
            throw new IllegalStateException("No passkey registered for Member '" + member + "'");
        }
        return passkey;
    }
}
