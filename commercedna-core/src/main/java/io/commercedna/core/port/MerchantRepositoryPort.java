package io.commercedna.core.port;

import io.commercedna.core.entity.Merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepositoryPort {
    Merchant save(Merchant merchant);
    Optional<Merchant> findById(UUID id);
    Optional<Merchant> findByMerchantCode(String merchantCode);
    boolean existsByMerchantCode(String merchantCode);
}
