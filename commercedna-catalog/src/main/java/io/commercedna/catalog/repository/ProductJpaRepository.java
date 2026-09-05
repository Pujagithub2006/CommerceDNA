package io.commercedna.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    List<ProductEntity> findByMerchantIdAndActiveTrue(UUID merchantId);

    Optional<ProductEntity> findByMerchantIdAndSku(UUID merchantId, String sku);

    boolean existsByMerchantIdAndSku(UUID merchantId, String sku);

    @Query("SELECT p FROM ProductEntity p WHERE p.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProductEntity> searchActiveProducts(@Param("query") String query);

    @Query("SELECT p FROM ProductEntity p WHERE p.merchantId = :merchantId AND p.active = true AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<ProductEntity> searchMerchantProducts(@Param("merchantId") UUID merchantId, @Param("query") String query);
}
