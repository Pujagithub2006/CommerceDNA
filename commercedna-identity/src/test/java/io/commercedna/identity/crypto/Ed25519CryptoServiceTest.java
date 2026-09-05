package io.commercedna.identity.crypto;

import io.commercedna.core.port.CryptoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519CryptoServiceTest {

    private Ed25519CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new Ed25519CryptoService();
    }

    @Test
    @DisplayName("Should generate valid Ed25519 keypair with non-empty Base64 strings")
    void shouldGenerateValidKeyPair() {
        CryptoPort.KeyPairResult keyPair = cryptoService.generateEd25519KeyPair();

        assertThat(keyPair).isNotNull();
        assertThat(keyPair.privateKeyBase64()).isNotBlank();
        assertThat(keyPair.publicKeyBase64()).isNotBlank();
        assertThat(keyPair.privateKeyBase64()).isNotEqualTo(keyPair.publicKeyBase64());
    }

    @Test
    @DisplayName("Should successfully sign and verify data using matching Ed25519 keypair")
    void shouldSignAndVerifySuccessfully() {
        CryptoPort.KeyPairResult keyPair = cryptoService.generateEd25519KeyPair();
        byte[] payload = "{\"proposalId\":\"p-12345\",\"pricePaise\":450000}".getBytes(StandardCharsets.UTF_8);

        String signature = cryptoService.sign(keyPair.privateKeyBase64(), payload);
        assertThat(signature).isNotBlank();

        boolean isValid = cryptoService.verify(keyPair.publicKeyBase64(), payload, signature);
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should fail verification when payload is modified (tamper detection)")
    void shouldRejectTamperedPayload() {
        CryptoPort.KeyPairResult keyPair = cryptoService.generateEd25519KeyPair();
        byte[] originalPayload = "{\"amount\":1000}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedPayload = "{\"amount\":1001}".getBytes(StandardCharsets.UTF_8);

        String signature = cryptoService.sign(keyPair.privateKeyBase64(), originalPayload);

        boolean isValid = cryptoService.verify(keyPair.publicKeyBase64(), tamperedPayload, signature);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should fail verification when signed with one key and verified with another")
    void shouldRejectSignatureFromDifferentPublicKey() {
        CryptoPort.KeyPairResult keyPair1 = cryptoService.generateEd25519KeyPair();
        CryptoPort.KeyPairResult keyPair2 = cryptoService.generateEd25519KeyPair();
        byte[] payload = "merchant-order-intent".getBytes(StandardCharsets.UTF_8);

        String signature = cryptoService.sign(keyPair1.privateKeyBase64(), payload);

        boolean isValid = cryptoService.verify(keyPair2.publicKeyBase64(), payload, signature);
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false on null or corrupted verification inputs")
    void shouldHandleNullOrCorruptedVerificationInputs() {
        CryptoPort.KeyPairResult keyPair = cryptoService.generateEd25519KeyPair();
        byte[] payload = "test-payload".getBytes(StandardCharsets.UTF_8);

        assertThat(cryptoService.verify(null, payload, "sig")).isFalse();
        assertThat(cryptoService.verify(keyPair.publicKeyBase64(), null, "sig")).isFalse();
        assertThat(cryptoService.verify(keyPair.publicKeyBase64(), payload, null)).isFalse();
        assertThat(cryptoService.verify("invalid-key-base64", payload, "invalid-sig-base64")).isFalse();
    }
}
