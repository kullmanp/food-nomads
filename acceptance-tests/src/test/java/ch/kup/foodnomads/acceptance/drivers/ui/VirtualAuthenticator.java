package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.Passkey;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import java.util.Base64;

/**
 * Chromium's virtual WebAuthn authenticator, driven over CDP: a platform authenticator with
 * resident keys that always reports the user as present and verified.
 */
final class VirtualAuthenticator {

    private final CDPSession cdp;
    private final String authenticatorId;

    VirtualAuthenticator(CDPSession cdp) {
        this.cdp = cdp;
        cdp.send("WebAuthn.enable");

        JsonObject options = new JsonObject();
        options.addProperty("protocol", "ctap2");
        options.addProperty("transport", "internal");
        options.addProperty("hasResidentKey", true);
        options.addProperty("hasUserVerification", true);
        options.addProperty("isUserVerified", true);
        options.addProperty("automaticPresenceSimulation", true);
        JsonObject params = new JsonObject();
        params.add("options", options);
        this.authenticatorId = cdp.send("WebAuthn.addVirtualAuthenticator", params)
                .get("authenticatorId").getAsString();
    }

    /** The one passkey on this authenticator. */
    Passkey onlyPasskey() {
        JsonObject params = new JsonObject();
        params.addProperty("authenticatorId", authenticatorId);
        JsonArray credentials = cdp.send("WebAuthn.getCredentials", params).getAsJsonArray("credentials");
        if (credentials.size() != 1) {
            throw new IllegalStateException("Expected one passkey on the authenticator, found " + credentials.size());
        }
        JsonObject credential = credentials.get(0).getAsJsonObject();
        return new Passkey(
                decode(credential.get("credentialId").getAsString()),
                decode(credential.get("privateKey").getAsString()),
                credential.get("rpId").getAsString(),
                decode(credential.get("userHandle").getAsString()),
                credential.get("signCount").getAsLong());
    }

    // CDP encodes binary fields as standard base64.
    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }
}
