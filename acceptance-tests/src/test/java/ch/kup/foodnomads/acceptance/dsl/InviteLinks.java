package ch.kup.foodnomads.acceptance.dsl;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** The links of the Invites a test created, by alias. One instance per test. */
final class InviteLinks {

    private final Map<String, URI> linkByAlias = new HashMap<>();

    void remember(String alias, URI link) {
        linkByAlias.put(alias, link);
    }

    URI of(String alias) {
        URI link = linkByAlias.get(alias);
        if (link == null) {
            throw new IllegalArgumentException("No Invite created with alias '" + alias + "'");
        }
        return link;
    }
}
