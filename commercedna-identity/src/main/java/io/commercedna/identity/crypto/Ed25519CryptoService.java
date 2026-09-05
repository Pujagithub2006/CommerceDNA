package io.commercedna.identity.crypto;

import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.core.port.CryptoPort;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

/**
 * High-performance Ed25519 Cryptographic Engine using native Java 25 cryptography.
 * Provides verifiable identity assertions, digital signatures, and tamper detection.
 */
@Service
public class Ed25519CryptoService implements CryptoPort {

    private static final String ALGORITHM = "Ed25519";

    @Override
    public KeyPairResult generateEd25519KeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM);
            KeyPair kp = kpg.generateKeyPair();

            String privateKeyBase64 = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
            String publicKeyBase64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

            return new KeyPairResult(privateKeyBase64, publicKeyBase64);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoVerificationException("Ed25519 algorithm not available in current runtime", e);
        }
    }

    @Override
    public String sign(String privateKeyBase64, byte[] data) {
        Objects.requireNonNull(privateKeyBase64, "privateKeyBase64 must not be null");
        Objects.requireNonNull(data, "data to sign must not be null");
        try {
            byte[] privBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privBytes);
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            PrivateKey privateKey = kf.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(data);

            byte[] sigBytes = signature.sign();
            return Base64.getEncoder().encodeToString(sigBytes);
        } catch (Exception e) {
            throw new CryptoVerificationException("Failed to sign data using Ed25519 private key: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verify(String publicKeyBase64, byte[] data, String signatureBase64) {
        if (publicKeyBase64 == null || data == null || signatureBase64 == null) {
            return false;
        }
        try {
            byte[] pubBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
            byte[] sigBytes = Base64.getDecoder().decode(signatureBase64.trim());

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubBytes);
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            PublicKey publicKey = kf.generatePublic(keySpec);

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);

            return signature.verify(sigBytes);
        } catch (Exception e) {
            return false;
        }
    }
}
