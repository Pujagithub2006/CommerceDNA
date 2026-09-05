package io.commercedna.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {
    Optional<MerchantEntity> findByMerchantCode(String merchantCode);
    boolean existsByMerchantCode(String merchantCode);
}
