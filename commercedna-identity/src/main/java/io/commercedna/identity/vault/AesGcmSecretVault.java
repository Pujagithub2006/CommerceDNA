package io.commercedna.identity.vault;

import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.core.port.SecretVaultPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * Production-grade AES-256-GCM symmetric encryption vault for sensitive credentials
 * (Razorpay Key ID, Razorpay Key Secret, and Webhook Secrets).
 * Guarantees authenticated confidentiality: any tampering throws CryptoVerificationException.
 */
@Service
public class AesGcmSecretVault implements SecretVaultPort {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKey masterSecretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmSecretVault(
            @Value("${commercedna.security.master-key:default_insecure_development_master_key_32_bytes_long!!}")
            String masterSecretString
    ) {
        Objects.requireNonNull(masterSecretString, "Master secret key string must not be null");
        this.masterSecretKey = derive256BitKey(masterSecretString);
    }

    @Override
    public String encrypt(String plainSecret) {
        if (plainSecret == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterSecretKey, gcmSpec);

            byte[] plainBytes = plainSecret.getBytes(StandardCharsets.UTF_8);
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            // Structure: [12-byte IV] + [Ciphertext with 16-byte GCM Tag]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new CryptoVerificationException("Failed to encrypt secret into AES-256-GCM vault: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String encryptedSecretBase64) {
        if (encryptedSecretBase64 == null) {
            return null;
        }
        try {
            byte[] combinedBytes = Base64.getDecoder().decode(encryptedSecretBase64.trim());
            if (combinedBytes.length < GCM_IV_LENGTH_BYTES + 16) {
                throw new CryptoVerificationException("Corrupted encrypted payload: payload too short.");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(combinedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterSecretKey, gcmSpec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CryptoVerificationException("Failed to decrypt secret or authentication tag verification failed: " + e.getMessage(), e);
        }
    }

    private static SecretKey derive256BitKey(String secretMaterial) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secretMaterial.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available in JVM", e);
        }
    }
}
