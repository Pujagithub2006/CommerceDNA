package io.commercedna.core.port;

/**
 * Port for symmetric encryption and secure key vault operations (AES-256-GCM).
 */
public interface SecretVaultPort {
    String encrypt(String plainSecret);
    String decrypt(String encryptedSecret);
}
