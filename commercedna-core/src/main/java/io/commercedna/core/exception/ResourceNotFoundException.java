package io.commercedna.core.exception;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(
            String.format("%s not found with identifier: %s", resourceType, identifier),
            "CDNA_RESOURCE_NOT_FOUND"
        );
    }
}
