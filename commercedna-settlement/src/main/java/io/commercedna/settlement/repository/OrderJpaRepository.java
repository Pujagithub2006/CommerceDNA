package io.commercedna.settlement.repository;

import io.commercedna.core.entity.OrderStatus;
import io.commercedna.settlement.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByOrderCode(String orderCode);

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<OrderEntity> findByRazorpayOrderId(String razorpayOrderId);

    Optional<OrderEntity> findByRazorpayPaymentLinkId(String razorpayPaymentLinkId);

    List<OrderEntity> findByMerchantId(UUID merchantId);

    List<OrderEntity> findByBuyerAgentDid(String buyerAgentDid);

    List<OrderEntity> findByStatus(OrderStatus status);
}
