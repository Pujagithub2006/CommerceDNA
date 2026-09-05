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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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
    private ProposalEntity proposal;

    @BeforeEach
    void setUp() {
        RazorpayProperties props = new RazorpayProperties();
        razorpayClient = new RazorpayClient(props);
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
                "apex@example.com",
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
                "Wireless headphones",
                "Electronics",
                "INR",
                499900L,
                250000L,
                0.15,
                0.50,
                1,
                100,
                50, // stock = 50
                "audio",
                true,
                Instant.now(),
                Instant.now()
        );

        proposal = new ProposalEntity(
                UUID.randomUUID(),
                "prop_test_001",
                merchantId,
                "did:cdna:buyer",
                "AURORA-ANC-001",
                10,
                440000L,
                null,
                4400000L,
                "INR",
                ProposalStatus.ACCEPTED,
                "Accepted",
                "pub_key",
                "sig_buyer",
                "sig_merchant",
                Instant.now().plus(15, ChronoUnit.MINUTES),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should atomically create order, decrement inventory, and generate Razorpay order and payment link")
    void shouldCreateOrderSuccessfully() {
        CreateOrderRequest request = new CreateOrderRequest(
                "idemp_key_001",
                "apex-tech",
                "did:cdna:buyer",
                "prop_test_001",
                "AURORA-ANC-001",
                10,
                440000L
        );

        when(orderRepository.findByIdempotencyKey("idemp_key_001")).thenReturn(Optional.empty());
        when(merchantIdentityService.findByMerchantCode("apex-tech")).thenReturn(Optional.of(merchant));
        when(productRepository.findByMerchantIdAndSku(merchantId, "AURORA-ANC-001")).thenReturn(Optional.of(product));
        when(proposalRepository.findByProposalCode("prop_test_001")).thenReturn(Optional.of(proposal));

        CreateOrderResponse response = orderService.createOrder(request);

        assertNotNull(response.orderId());
        assertTrue(response.orderCode().startsWith("ord_"));
        assertEquals("apex-tech", response.merchantCode());
        assertEquals("AURORA-ANC-001", response.sku());
        assertEquals(10, response.quantity());
        assertEquals(4400000L, response.totalAmountPaise());
        assertEquals(OrderStatus.CREATED, response.status());
        assertNotNull(response.razorpayOrderId());
        assertTrue(response.paymentLinkUrl().startsWith("https://rzp.io/i/"));

        // Verify stock decremented from 50 to 40
        assertEquals(40, product.getStockQuantity());
        verify(productRepository, times(1)).save(product);
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
        verify(outboxRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should reject order when inventory is exhausted")
    void shouldRejectWhenStockExhausted() {
        product.setStockQuantity(2); // Only 2 in stock

        CreateOrderRequest request = new CreateOrderRequest(
                "idemp_key_002",
                "apex-tech",
                "did:cdna:buyer",
                null,
                "AURORA-ANC-001",
                10,
                440000L
        );

        when(orderRepository.findByIdempotencyKey("idemp_key_002")).thenReturn(Optional.empty());
        when(merchantIdentityService.findByMerchantCode("apex-tech")).thenReturn(Optional.of(merchant));
        when(productRepository.findByMerchantIdAndSku(merchantId, "AURORA-ANC-001")).thenReturn(Optional.of(product));

        assertThrows(InventoryExhaustedException.class, () -> orderService.createOrder(request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should detect idempotency conflict if same key used for different payload")
    void shouldDetectIdempotencyConflict() {
        OrderEntity existingOrder = new OrderEntity(
                UUID.randomUUID(),
                "ord_existing",
                merchantId,
                "did:cdna:buyer",
                null,
                "idemp_key_conflict",
                "AURORA-ANC-001",
                5, // original quantity = 5
                440000L,
                2200000L,
                "INR",
                OrderStatus.CREATED,
                "order_123",
                null,
                "plink_123",
                "url",
                Instant.now(),
                Instant.now()
        );

        when(orderRepository.findByIdempotencyKey("idemp_key_conflict")).thenReturn(Optional.of(existingOrder));

        CreateOrderRequest modifiedRequest = new CreateOrderRequest(
                "idemp_key_conflict",
                "apex-tech",
                "did:cdna:buyer",
                null,
                "AURORA-ANC-001",
                10, // modified quantity = 10 -> conflict!
                440000L
        );

        assertThrows(IdempotencyConflictException.class, () -> orderService.createOrder(modifiedRequest));
    }
}
