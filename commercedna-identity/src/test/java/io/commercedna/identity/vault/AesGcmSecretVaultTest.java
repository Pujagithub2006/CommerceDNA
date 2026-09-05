package io.commercedna.identity.vault;

import io.commercedna.core.exception.CryptoVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSecretVaultTest {

    private AesGcmSecretVault vault;

    @BeforeEach
    void setUp() {
        vault = new AesGcmSecretVault("super_secret_test_master_encryption_key_2026!");
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt Razorpay API credentials")
    void shouldEncryptAndDecryptSuccessfully() {
        String originalSecret = "rzp_test_secret_abc123xyz890";

        String ciphertext = vault.encrypt(originalSecret);
        assertThat(ciphertext).isNotBlank();
        assertThat(ciphertext).isNotEqualTo(originalSecret);

        String decrypted = vault.decrypt(ciphertext);
        assertThat(decrypted).isEqualTo(originalSecret);
    }

    @Test
    @DisplayName("Should produce different ciphertexts for identical plaintext due to random IV")
    void shouldProduceDifferentCiphertextsForSameInput() {
        String secret = "rzp_live_secret_identical_test";

        String cipher1 = vault.encrypt(secret);
        String cipher2 = vault.encrypt(secret);

        assertThat(cipher1).isNotEqualTo(cipher2);
        assertThat(vault.decrypt(cipher1)).isEqualTo(secret);
        assertThat(vault.decrypt(cipher2)).isEqualTo(secret);
    }

    @Test
    @DisplayName("Should throw CryptoVerificationException when ciphertext or authentication tag is tampered")
    void shouldThrowExceptionOnTamperedCiphertext() {
        String originalSecret = "webhook_secret_hmac_sha256";
        String ciphertext = vault.encrypt(originalSecret);

        byte[] rawBytes = Base64.getDecoder().decode(ciphertext);
        // Corrupt the last byte (part of the 128-bit authentication tag)
        rawBytes[rawBytes.length - 1] ^= 0xFF;
        String tamperedCiphertext = Base64.getEncoder().encodeToString(rawBytes);

        assertThatThrownBy(() -> vault.decrypt(tamperedCiphertext))
                .isInstanceOf(CryptoVerificationException.class)
                .hasMessageContaining("Failed to decrypt secret or authentication tag verification failed");
    }

    @Test
    @DisplayName("Should throw CryptoVerificationException on truncated or corrupted payload")
    void shouldThrowExceptionOnTruncatedPayload() {
        String truncated = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        assertThatThrownBy(() -> vault.decrypt(truncated))
                .isInstanceOf(CryptoVerificationException.class)
                .hasMessageContaining("Corrupted encrypted payload");
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
        assertThat(vault.encrypt(null)).isNull();
        assertThat(vault.decrypt(null)).isNull();
    }
}
