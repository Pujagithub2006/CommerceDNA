package io.commercedna.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.api.dto.CreateProductRequest;
import io.commercedna.api.dto.RegisterMerchantRequest;
import io.commercedna.core.entity.OrderStatus;
import io.commercedna.settlement.dto.CreateOrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String merchantCode;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        merchantCode = "settle_m_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterMerchantRequest merchantReq = new RegisterMerchantRequest(
                merchantCode,
                "Settlement Merchant",
                merchantCode + "@settle.io",
                "rzp_test_settle1",
                "rzp_test_settle2",
                "webhook_sec_settle"
        );

        MvcResult mResult = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> mMap = objectMapper.readValue(mResult.getResponse().getContentAsString(), Map.class);
        merchantId = (String) mMap.get("merchantId");

        CreateProductRequest pReq = new CreateProductRequest(
                "SETTLE-SKU-01",
                "Settlement Watch",
                "Precision watch",
                "Watches",
                "INR",
                new BigDecimal("10000.00"),
                new BigDecimal("6000.00"),
                0.15,
                0.30,
                1,
                50,
                100,
                "watch"
        );

        mockMvc.perform(post("/api/v1/merchants/" + merchantId + "/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pReq)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/v1/settlement/orders - should create order with Razorpay link and retrieve it")
    void shouldCreateAndRetrieveOrder() throws Exception {
        String idempKey = "idemp_" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                idempKey,
                merchantCode,
                "did:cdna:buyer-settle",
                null,
                "SETTLE-SKU-01",
                5,
                900000L // 9,000 INR
        );

        MvcResult result = mockMvc.perform(post("/api/v1/settlement/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantCode").value(merchantCode))
                .andExpect(jsonPath("$.sku").value("SETTLE-SKU-01"))
                .andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.totalAmountPaise").value(4500000L))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.razorpayOrderId").isString())
                .andExpect(jsonPath("$.paymentLinkUrl").isString())
                .andReturn();

        Map<?, ?> respMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String orderCode = (String) respMap.get("orderCode");

        // Verify GET /api/v1/settlement/orders/{code}
        mockMvc.perform(get("/api/v1/settlement/orders/" + orderCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode").value(orderCode))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/razorpay - should verify HMAC signature and acknowledge webhook")
    void shouldHandleWebhook() throws Exception {
        // First create an order to get a valid Razorpay Order ID
        String idempKey = "idemp_webhook_" + UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest(
                idempKey,
                merchantCode,
                "did:cdna:buyer-webhook",
                null,
                "SETTLE-SKU-01",
                2,
                900000L
        );

        MvcResult result = mockMvc.perform(post("/api/v1/settlement/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> respMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String rzpOrderId = (String) respMap.get("razorpayOrderId");
        String orderCode = (String) respMap.get("orderCode");

        String payload = """
                {
                  "entity": "event",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test_webhook_001",
                        "order_id": "%s",
                        "amount": 1800000,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """.formatted(rzpOrderId);

        // Compute HMAC using default test secret "cdna_webhook_test_secret"
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("cdna_webhook_test_secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                        .header("X-Razorpay-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.newStatus").value("PAID"));

        // Verify order is now PAID
        mockMvc.perform(get("/api/v1/settlement/orders/" + orderCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.razorpayPaymentId").value("pay_test_webhook_001"));

        // Test Refund API
        io.commercedna.settlement.dto.RefundRequest refundReq = new io.commercedna.settlement.dto.RefundRequest(
                orderCode,
                "Customer requested return within policy window"
        );

        mockMvc.perform(post("/api/v1/settlement/refunds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderCode").value(orderCode))
                .andExpect(jsonPath("$.status").value("REFUNDED"))
                .andExpect(jsonPath("$.refundId").isNotEmpty())
                .andExpect(jsonPath("$.refundedAmountPaise").value(1800000L));

        // Verify order status is REFUNDED
        mockMvc.perform(get("/api/v1/settlement/orders/" + orderCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }
}
