package ch.kup.foodnomads.acceptance.drivers.ui;

import ch.kup.foodnomads.acceptance.drivers.Passkey;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.playwright.CDPSession;
import java.util.Arrays;
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
        JsonArray credentials = credentials();
        if (credentials.size() != 1) {
            throw new IllegalStateException("Expected one passkey on the authenticator, found " + credentials.size());
        }
        return toPasskey(credentials.get(0).getAsJsonObject());
    }

    /** The passkey with this credential id, as it is now on this authenticator, sign count included. */
    Passkey passkey(byte[] credentialId) {
        for (var credential : credentials()) {
            if (Arrays.equals(credentialId, decode(credential.getAsJsonObject().get("credentialId").getAsString()))) {
                return toPasskey(credential.getAsJsonObject());
            }
        }
        throw new IllegalStateException("The passkey isn't on the authenticator");
    }

    /** Puts this passkey on the authenticator as a discoverable credential, unless it is there already. */
    void holdPasskey(Passkey passkey) {
        for (var credential : credentials()) {
            if (Arrays.equals(passkey.credentialId(),
                    decode(credential.getAsJsonObject().get("credentialId").getAsString()))) {
                return;
            }
        }
        JsonObject credential = new JsonObject();
        credential.addProperty("credentialId", encode(passkey.credentialId()));
        credential.addProperty("isResidentCredential", true);
        credential.addProperty("rpId", passkey.rpId());
        credential.addProperty("privateKey", encode(passkey.privateKey()));
        credential.addProperty("userHandle", encode(passkey.userHandle()));
        credential.addProperty("signCount", passkey.signCount());
        JsonObject params = new JsonObject();
        params.addProperty("authenticatorId", authenticatorId);
        params.add("credential", credential);
        cdp.send("WebAuthn.addCredential", params);
    }

    private JsonArray credentials() {
        JsonObject params = new JsonObject();
        params.addProperty("authenticatorId", authenticatorId);
        return cdp.send("WebAuthn.getCredentials", params).getAsJsonArray("credentials");
    }

    private static Passkey toPasskey(JsonObject credential) {
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

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
