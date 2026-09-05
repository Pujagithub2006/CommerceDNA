package io.commercedna.core.port;

/**
 * Port for asymmetric cryptographic operations (Ed25519) and digital signature verification.
 */
public interface CryptoPort {
    record KeyPairResult(String privateKeyBase64, String publicKeyBase64) {}

    KeyPairResult generateEd25519KeyPair();
    String sign(String privateKeyBase64, byte[] data);
    boolean verify(String publicKeyBase64, byte[] data, String signatureBase64);
}
