package io.commercedna.settlement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.repository.ProductJpaRepository;
import io.commercedna.core.entity.Merchant;
import io.commercedna.core.entity.OrderStatus;
import io.commercedna.core.entity.ProposalStatus;
import io.commercedna.core.exception.IdempotencyConflictException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.identity.service.MerchantIdentityService;
import io.commercedna.negotiation.repository.ProposalEntity;
import io.commercedna.negotiation.repository.ProposalJpaRepository;
import io.commercedna.settlement.client.RazorpayClient;
import io.commercedna.settlement.dto.CreateOrderRequest;
import io.commercedna.settlement.dto.CreateOrderResponse;
import io.commercedna.settlement.dto.CreatePaymentLinkRequest;
import io.commercedna.settlement.dto.PaymentLinkResponse;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.entity.OutboxEventEntity;
import io.commercedna.settlement.repository.OrderJpaRepository;
import io.commercedna.settlement.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Atomic Order Compilation and Settlement State Machine.
 * Manages inventory reservations, Razorpay order/payment link creation, and transactional outbox.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderJpaRepository orderRepository;
    private final OutboxJpaRepository outboxRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final ProductJpaRepository productRepository;
    private final ProposalJpaRepository proposalRepository;
    private final RazorpayClient razorpayClient;
    private final ObjectMapper objectMapper;

    public OrderService(
            OrderJpaRepository orderRepository,
            OutboxJpaRepository outboxRepository,
            MerchantIdentityService merchantIdentityService,
            ProductJpaRepository productRepository,
            ProposalJpaRepository proposalRepository,
            RazorpayClient razorpayClient,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.merchantIdentityService = Objects.requireNonNull(merchantIdentityService);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.proposalRepository = Objects.requireNonNull(proposalRepository);
        this.razorpayClient = Objects.requireNonNull(razorpayClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public CreateOrderResponse createOrder(CreateOrderRequest req) {
        Objects.requireNonNull(req, "CreateOrderRequest must not be null");

        // 1. Idempotency Check
        Optional<OrderEntity> existing = orderRepository.findByIdempotencyKey(req.idempotencyKey().trim());
        if (existing.isPresent()) {
            OrderEntity order = existing.get();
            if (!order.getSku().equalsIgnoreCase(req.sku().trim())
                    || !Objects.equals(order.getQuantity(), req.quantity())
                    || !Objects.equals(order.getUnitPricePaise(), req.unitPricePaise())) {
                throw new IdempotencyConflictException(req.idempotencyKey());
            }
            return mapToResponse(order, req.merchantCode());
        }

        // 2. Merchant Lookup
        Merchant merchant = merchantIdentityService.findByMerchantCode(req.merchantCode().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant with code", req.merchantCode()));

        // 3. Product Lookup & Atomic Stock Check
        ProductEntity product = productRepository.findByMerchantIdAndSku(merchant.getId(), req.sku().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU", req.sku()));

        if (product.getStockQuantity() < req.quantity()) {
            throw new InventoryExhaustedException(product.getSku(), req.quantity(), product.getStockQuantity());
        }

        // 4. Validate Proposal (if supplied)
        if (req.proposalCode() != null && !req.proposalCode().isBlank()) {
            ProposalEntity proposal = proposalRepository.findByProposalCode(req.proposalCode().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Proposal", req.proposalCode()));
            if (proposal.getStatus() != ProposalStatus.ACCEPTED && proposal.getStatus() != ProposalStatus.COUNTERED) {
                throw new IllegalStateException("Cannot create order from proposal with status: " + proposal.getStatus());
            }
            if (proposal.getExpiresAt().isBefore(Instant.now())) {
                throw new IllegalStateException("Proposal lock has expired.");
            }
        }

        // 5. Reserve stock atomically
        product.setStockQuantity(product.getStockQuantity() - req.quantity());
        productRepository.save(product);

        long totalAmountPaise = req.quantity() * req.unitPricePaise();
        UUID orderId = UUID.randomUUID();
        String orderCode = "ord_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 6. Invoke Razorpay Test Mode Client
        RazorpayClient.RazorpayOrderResult rzpOrder = razorpayClient.createOrder(
                totalAmountPaise,
                product.getCurrency(),
                orderCode,
                Map.of(
                        "merchantCode", merchant.getMerchantCode(),
                        "buyerAgentDid", req.buyerAgentDid(),
                        "sku", product.getSku(),
                        "quantity", String.valueOf(req.quantity())
                )
        );

        RazorpayClient.RazorpayPaymentLinkResult rzpLink = razorpayClient.createPaymentLink(
                totalAmountPaise,
                product.getCurrency(),
                "CommerceDNA Order: " + product.getTitle(),
                "AI Agent Buyer",
                "buyer@agent.network",
                Map.of("orderCode", orderCode)
        );

        Instant now = Instant.now();
        OrderEntity entity = new OrderEntity(
                orderId,
                orderCode,
                merchant.getId(),
                req.buyerAgentDid().trim(),
                req.proposalCode(),
                req.idempotencyKey().trim(),
                product.getSku(),
                req.quantity(),
                req.unitPricePaise(),
                totalAmountPaise,
                product.getCurrency(),
                OrderStatus.CREATED,
                rzpOrder.id(),
                null,
                rzpLink.id(),
                rzpLink.shortUrl(),
                now,
                now
        );

        orderRepository.save(entity);

        // 7. Enqueue Transactional Outbox Event
        try {
            String payloadJson = objectMapper.writeValueAsString(mapToResponse(entity, merchant.getMerchantCode()));
            OutboxEventEntity outboxEvent = new OutboxEventEntity(
                    UUID.randomUUID(),
                    "ORDER_CREATED",
                    "ORDER",
                    entity.getId().toString(),
                    payloadJson,
                    "PENDING",
                    0,
                    now,
                    null
            );
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize outbox event payload", ex);
        }

        return mapToResponse(entity, merchant.getMerchantCode());
    }

    public PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest req) {
        OrderEntity order = orderRepository.findByOrderCode(req.orderCode().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order", req.orderCode()));

        if (order.getPaymentLinkUrl() == null || order.getPaymentLinkUrl().isBlank()) {
            RazorpayClient.RazorpayPaymentLinkResult link = razorpayClient.createPaymentLink(
                    order.getTotalAmountPaise(),
                    order.getCurrency(),
                    "CommerceDNA Order: " + order.getSku(),
                    req.customerName() != null ? req.customerName() : "AI Buyer",
                    req.customerEmail() != null ? req.customerEmail() : "buyer@agent.network",
                    Map.of("orderCode", order.getOrderCode())
            );
            order.setRazorpayPaymentLinkId(link.id());
            order.setPaymentLinkUrl(link.shortUrl());
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);
        }

        return new PaymentLinkResponse(
                order.getOrderCode(),
                order.getRazorpayPaymentLinkId(),
                order.getPaymentLinkUrl(),
                order.getTotalAmountPaise(),
                order.getCurrency(),
                order.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderCode));
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
    }

    @Transactional(readOnly = true)
    public List<OrderEntity> getOrdersByMerchant(UUID merchantId) {
        return orderRepository.findByMerchantId(merchantId);
    }

    private CreateOrderResponse mapToResponse(OrderEntity entity, String merchantCode) {
        return new CreateOrderResponse(
                entity.getId(),
                entity.getOrderCode(),
                merchantCode,
                entity.getBuyerAgentDid(),
                entity.getProposalCode(),
                entity.getSku(),
                entity.getQuantity(),
                entity.getUnitPricePaise(),
                entity.getTotalAmountPaise(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getRazorpayOrderId(),
                entity.getPaymentLinkUrl(),
                entity.getCreatedAt()
        );
    }
}
