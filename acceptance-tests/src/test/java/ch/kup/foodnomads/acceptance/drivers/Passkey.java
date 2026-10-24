package ch.kup.foodnomads.acceptance.drivers;

/**
 * A passkey's private side, in the shape both authenticators understand: the Chromium virtual
 * authenticator ({@code WebAuthn.addCredential}) and the API driver's software authenticator.
 *
 * @param credentialId raw credential id
 * @param privateKey   EC P-256 private key, PKCS#8
 * @param userHandle   the user handle the relying party assigned at registration
 */
public record Passkey(byte[] credentialId, byte[] privateKey, String rpId, byte[] userHandle, long signCount) {
}
