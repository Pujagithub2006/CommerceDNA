package io.commercedna.core.exception;

public class CryptoVerificationException extends DomainException {
    public CryptoVerificationException(String message) {
        super(message, "CDNA_CRYPTO_VERIFICATION_FAILED");
    }

    public CryptoVerificationException(String message, Throwable cause) {
        super(message, "CDNA_CRYPTO_VERIFICATION_FAILED", cause);
    }
}
