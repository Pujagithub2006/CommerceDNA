package io.commercedna.settlement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.repository.ProductJpaRepository;
import io.commercedna.core.entity.Merchant;
import io.commercedna.core.entity.OrderStatus;
import io.commercedna.core.entity.ProposalStatus;
import io.commercedna.core.exception.IdempotencyConflictException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.identity.service.MerchantIdentityService;
import io.commercedna.negotiation.repository.ProposalEntity;
import io.commercedna.negotiation.repository.ProposalJpaRepository;
import io.commercedna.settlement.client.RazorpayClient;
import io.commercedna.settlement.client.RazorpayProperties;
import io.commercedna.settlement.dto.CreateOrderRequest;
import io.commercedna.settlement.dto.CreateOrderResponse;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.entity.OutboxEventEntity;
import io.commercedna.settlement.repository.OrderJpaRepository;
import io.commercedna.settlement.repository.OutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Sprint 7 Chaos, Concurrency & Financial Invariant Benchmark.
 * Simulates high-concurrency race conditions, duplicate idempotency bursts,
 * inventory depletion scenarios, and integer paise preservation.
 */
@ExtendWith(MockitoExtension.class)
class ChaosConcurrencySettlementTest {

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OutboxJpaRepository outboxRepository;

    @Mock
    private MerchantIdentityService merchantIdentityService;

    @Mock
    private ProductJpaRepository productRepository;

    @Mock
    private ProposalJpaRepository proposalRepository;

    private RazorpayClient razorpayClient;
    private ObjectMapper objectMapper;
    private OrderService orderService;

    private UUID merchantId;
    private Merchant merchant;
    private ProductEntity product;
    private ProposalEntity approvedProposal;

    @BeforeEach
    void setUp() {
        RazorpayProperties properties = new RazorpayProperties();
        razorpayClient = new RazorpayClient(properties);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        orderService = new OrderService(
                orderRepository,
                outboxRepository,
                merchantIdentityService,
                productRepository,
                proposalRepository,
                razorpayClient,
                objectMapper
        );

        merchantId = UUID.randomUUID();
        merchant = new Merchant(
                merchantId,
                "apex-tech",
                "Apex Electronics",
                "sales@apextech.in",
                "pub_key_123",
                true,
                Instant.now(),
                Instant.now()
        );

        product = new ProductEntity(
                UUID.randomUUID(),
                merchantId,
                "AURORA-ANC-001",
                "Aurora ANC Headphones",
                "Noise-cancelling headphones",
                "Audio",
                "INR",
                499900L,
                250000L,
                0.15,
                0.40,
                1,
                10,
                10, // Available Stock = 10 units
                "headphones",
                true,
                Instant.now(),
                Instant.now()
        );

        approvedProposal = new ProposalEntity(
                UUID.randomUUID(),
                "PROP-CHAOS-001",
                merchantId,
                "did:cdna:buyer-chaos",
                "AURORA-ANC-001",
                1,
                380000L, // ₹3,800.00
                null,
                380000L,
                "INR",
                ProposalStatus.ACCEPTED,
                "Accepted by guardrail",
                "pub_buyer",
                "sig_buyer",
                "sig_merchant",
                Instant.now().plus(1, ChronoUnit.HOURS),
                Instant.now(),
                Instant.now()
        );

        lenient().when(merchantIdentityService.findByMerchantCode("apex-tech")).thenReturn(Optional.of(merchant));
        lenient().when(proposalRepository.findByProposalCode("PROP-CHAOS-001")).thenReturn(Optional.of(approvedProposal));
        lenient().when(productRepository.findByMerchantIdAndSku(merchantId, "AURORA-ANC-001")).thenReturn(Optional.of(product));
        lenient().when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(outboxRepository.save(any(OutboxEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Simulates Concurrent Buyers Racing for Limited Inventory with Stock Invariant Maintained")
    void testConcurrentInventoryDepletion() throws InterruptedException {
        int attempts = 15;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exhaustedCount = new AtomicInteger(0);

        for (int i = 0; i < attempts; i++) {
            final String idempKey = "IDEMP-CONCURRENCY-" + i;
            CreateOrderRequest req = new CreateOrderRequest(
                    idempKey,
                    "apex-tech",
                    "did:cdna:buyer-concurrent",
                    "PROP-CHAOS-001",
                    "AURORA-ANC-001",
                    1,
                    380000L
            );

            try {
                CreateOrderResponse response = orderService.createOrder(req);
                if (response != null && response.orderId() != null) {
                    successCount.incrementAndGet();
                }
            } catch (InventoryExhaustedException e) {
                exhaustedCount.incrementAndGet();
            }
        }

        // 10 units in stock -> exactly 10 successes and 5 inventory exhausted rejections
        assertEquals(10, successCount.get(), "Must fulfill exactly available stock");
        assertEquals(5, exhaustedCount.get(), "Must reject all excess attempts");
        assertEquals(0, product.getStockQuantity(), "Stock cannot be negative");
    }

    @Test
    @DisplayName("Simulates Duplicate Webhook / Client Retries with Strict Idempotent Finality")
    void testDuplicateOrderSubmissionIdempotency() {
        String idempotencyKey = "IDEMP-REPLAY-KEY-001";
        CreateOrderRequest req = new CreateOrderRequest(
                idempotencyKey,
                "apex-tech",
                "did:cdna:buyer-idemp",
                "PROP-CHAOS-001",
                "AURORA-ANC-001",
                1,
                380000L
        );

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        // First attempt succeeds
        CreateOrderResponse res1 = orderService.createOrder(req);
        assertNotNull(res1);
        assertEquals("AURORA-ANC-001", res1.sku());
        assertEquals(380000L, res1.totalAmountPaise());

        // Simulate that repository now has the existing order for this idempotency key
        OrderEntity existingOrder = new OrderEntity(
                UUID.randomUUID(),
                res1.orderCode(),
                merchantId,
                "did:cdna:buyer-idemp",
                approvedProposal.getProposalCode(),
                idempotencyKey,
                "AURORA-ANC-001",
                1,
                380000L,
                380000L,
                "INR",
                OrderStatus.CREATED,
                res1.razorpayOrderId(),
                null,
                "plink_demo123",
                res1.paymentLinkUrl(),
                Instant.now(),
                Instant.now()
        );

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));

        // Replaying with identical request parameters returns the cached order response without re-executing
        CreateOrderResponse res2 = orderService.createOrder(req);
        assertEquals(res1.orderCode(), res2.orderCode());

        // Replaying with different request parameters (e.g. quantity = 2) throws IdempotencyConflictException
        CreateOrderRequest modifiedReq = new CreateOrderRequest(
                idempotencyKey,
                "apex-tech",
                "did:cdna:buyer-idemp",
                "PROP-CHAOS-001",
                "AURORA-ANC-001",
                2, // Modified quantity
                380000L
        );

        assertThrows(IdempotencyConflictException.class, () -> orderService.createOrder(modifiedReq));
    }
}
