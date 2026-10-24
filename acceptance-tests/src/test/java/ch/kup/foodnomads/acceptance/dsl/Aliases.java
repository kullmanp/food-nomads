package ch.kup.foodnomads.acceptance.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Functional isolation: tests use readable aliases ({@code "Sternen"}) that the DSL turns into
 * names unique to this test ({@code "Sternen 3f9a1c"}), so tests can share one running app.
 * One instance per test.
 */
public final class Aliases {

    private final String suffix = UUID.randomUUID().toString().substring(0, 6);
    private final Map<String, String> realNameByAlias = new HashMap<>();
    private final Map<String, String> aliasByRealName = new HashMap<>();

    public String realName(String alias) {
        if (alias.isEmpty()) {
            return alias;
        }
        return realNameByAlias.computeIfAbsent(alias, a -> {
            String realName = a + " " + suffix;
            aliasByRealName.put(realName, a);
            return realName;
        });
    }

    /** The alias a real name was created from, or the real name itself if it isn't one of ours. */
    public String aliasOf(String realName) {
        return aliasByRealName.getOrDefault(realName, realName);
    }
}
