package ch.kup.foodnomads.acceptance.drivers.api;

import ch.kup.foodnomads.acceptance.drivers.Passkey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * A minimal WebAuthn authenticator in Java: ES256 keys, "none" attestation, user present and
 * verified. Enough for the API driver to run the real ceremonies against the app.
 */
final class SoftwareAuthenticator {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final CBORFactory CBOR = new CBORFactory();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int FLAG_USER_PRESENT = 0x01;
    private static final int FLAG_USER_VERIFIED = 0x04;
    private static final int FLAG_ATTESTED_CREDENTIAL_DATA = 0x40;

    record Registration(ObjectNode credential, Passkey passkey) {
    }

    private SoftwareAuthenticator() {
    }

    /**
     * Answers {@code navigator.credentials.create()} for the given creation options.
     *
     * @param options the {@code PublicKeyCredentialCreationOptions} JSON from the app
     * @param origin  the origin the "browser" is on, e.g. {@code http://localhost:8080}
     */
    static Registration register(JsonNode options, String origin) {
        String rpId = options.path("rp").path("id").asText();
        byte[] userHandle = decode(options.path("user").path("id").asText());
        byte[] credentialId = new byte[32];
        RANDOM.nextBytes(credentialId);
        KeyPair keyPair = newKeyPair();

        byte[] clientDataJson = clientData("webauthn.create", options.path("challenge").asText(), origin);
        byte[] authenticatorData = authenticatorData(rpId, credentialId, (ECPublicKey) keyPair.getPublic());
        byte[] attestationObject = attestationObject(authenticatorData);

        ObjectNode credential = JSON.createObjectNode();
        credential.put("id", encode(credentialId));
        credential.put("rawId", encode(credentialId));
        credential.put("type", "public-key");
        ObjectNode response = credential.putObject("response");
        response.put("attestationObject", encode(attestationObject));
        response.put("clientDataJSON", encode(clientDataJson));

        Passkey passkey = new Passkey(credentialId, keyPair.getPrivate().getEncoded(), rpId, userHandle, 0);
        return new Registration(credential, passkey);
    }

    /**
     * Answers {@code navigator.credentials.get()} for the given request options with this
     * passkey. The sign count stays 0, like authenticators that don't count.
     *
     * @param options the {@code PublicKeyCredentialRequestOptions} JSON from the app
     * @param origin  the origin the "browser" is on, e.g. {@code http://localhost:8080}
     */
    static ObjectNode logIn(JsonNode options, Passkey passkey, String origin) {
        byte[] clientDataJson = clientData("webauthn.get", options.path("challenge").asText(), origin);
        byte[] authenticatorData = ByteBuffer.allocate(32 + 1 + 4)
                .put(sha256(options.path("rpId").asText().getBytes(StandardCharsets.UTF_8)))
                .put((byte) (FLAG_USER_PRESENT | FLAG_USER_VERIFIED))
                .putInt(0)
                .array();
        byte[] signature = sign(passkey.privateKey(), concat(authenticatorData, sha256(clientDataJson)));

        ObjectNode credential = JSON.createObjectNode();
        credential.put("id", encode(passkey.credentialId()));
        credential.put("rawId", encode(passkey.credentialId()));
        credential.put("type", "public-key");
        ObjectNode response = credential.putObject("response");
        response.put("clientDataJSON", encode(clientDataJson));
        response.put("authenticatorData", encode(authenticatorData));
        response.put("signature", encode(signature));
        response.put("userHandle", encode(passkey.userHandle()));
        return credential;
    }

    /** ES256: ECDSA over SHA-256, DER-encoded as WebAuthn expects. */
    private static byte[] sign(byte[] pkcs8PrivateKey, byte[] data) {
        try {
            PrivateKey key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8PrivateKey));
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(key);
            signature.update(data);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] concat(byte[] first, byte[] second) {
        return ByteBuffer.allocate(first.length + second.length).put(first).put(second).array();
    }

    private static byte[] clientData(String type, String challenge, String origin) {
        ObjectNode clientData = JSON.createObjectNode();
        clientData.put("type", type);
        clientData.put("challenge", challenge);
        clientData.put("origin", origin);
        clientData.put("crossOrigin", false);
        return clientData.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] authenticatorData(String rpId, byte[] credentialId, ECPublicKey publicKey) {
        byte[] coseKey = coseKey(publicKey);
        return ByteBuffer.allocate(32 + 1 + 4 + 16 + 2 + credentialId.length + coseKey.length)
                .put(sha256(rpId.getBytes(StandardCharsets.UTF_8)))
                .put((byte) (FLAG_USER_PRESENT | FLAG_USER_VERIFIED | FLAG_ATTESTED_CREDENTIAL_DATA))
                .putInt(0)
                .put(new byte[16])
                .putShort((short) credentialId.length)
                .put(credentialId)
                .put(coseKey)
                .array();
    }

    private static byte[] coseKey(ECPublicKey publicKey) {
        return cbor(gen -> {
            gen.writeStartObject();
            gen.writeFieldId(1); // kty: EC2
            gen.writeNumber(2);
            gen.writeFieldId(3); // alg: ES256
            gen.writeNumber(-7);
            gen.writeFieldId(-1); // crv: P-256
            gen.writeNumber(1);
            gen.writeFieldId(-2);
            gen.writeBinary(unsigned32(publicKey.getW().getAffineX()));
            gen.writeFieldId(-3);
            gen.writeBinary(unsigned32(publicKey.getW().getAffineY()));
            gen.writeEndObject();
        });
    }

    private static byte[] attestationObject(byte[] authenticatorData) {
        return cbor(gen -> {
            gen.writeStartObject();
            gen.writeStringField("fmt", "none");
            gen.writeFieldName("attStmt");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeFieldName("authData");
            gen.writeBinary(authenticatorData);
            gen.writeEndObject();
        });
    }

    private interface CborWriter {
        void write(CBORGenerator gen) throws IOException;
    }

    private static byte[] cbor(CborWriter writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (CBORGenerator gen = CBOR.createGenerator(out)) {
            writer.write(gen);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static byte[] unsigned32(BigInteger value) {
        byte[] bytes = value.toByteArray();
        byte[] result = new byte[32];
        int length = Math.min(bytes.length, 32);
        System.arraycopy(bytes, bytes.length - length, result, 32 - length, length);
        return result;
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] decode(String base64Url) {
        return Base64.getUrlDecoder().decode(base64Url);
    }
}
