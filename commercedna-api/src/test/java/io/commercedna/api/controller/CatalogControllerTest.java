package io.commercedna.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.api.dto.CreateProductRequest;
import io.commercedna.api.dto.RegisterMerchantRequest;
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

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        String code = "catalog_merchant_" + UUID.randomUUID().toString().substring(0, 8);
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                code,
                "Catalog Retail Ltd",
                code + "@retail.co.in",
                "rzp_test_cat1",
                "rzp_test_cat2",
                "webhook_sec_cat"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> map = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        merchantId = (String) map.get("merchantId");
    }

    @Test
    @DisplayName("Should successfully create product and search via agentic catalog search endpoint")
    void shouldCreateProductAndSearch() throws Exception {
        CreateProductRequest request = new CreateProductRequest(
                "WATCH-AUTO-77",
                "Seiko Automatic Diver Watch",
                "200m water resistance automatic dive watch",
                "Watches",
                "INR",
                new BigDecimal("28000.00"), // base: 28,000 INR
                new BigDecimal("19000.00"), // cost: 19,000 INR
                0.15,                       // 15% min margin
                0.20,                       // 20% max discount
                1,
                10,
                25,
                "diver,seiko,automatic,watches"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/merchants/" + merchantId + "/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.sku").value("WATCH-AUTO-77"))
                .andExpect(jsonPath("$.basePricePaise").value(2800000L))
                .andExpect(jsonPath("$.inStock").value(true))
                .andReturn();

        Map<?, ?> productMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String productId = (String) productMap.get("id");

        // Verify GET /api/v1/merchants/{merchantId}/products/{productId}
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Seiko Automatic Diver Watch"));

        // Verify GET /api/v1/catalog/search?q=diver
        mockMvc.perform(get("/api/v1/catalog/search")
                        .param("q", "diver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("WATCH-AUTO-77"));
    }

    @Test
    @DisplayName("Should reject product creation when base price is lower than cost price")
    void shouldRejectWhenBasePriceLessThanCostPrice() throws Exception {
        CreateProductRequest invalid = new CreateProductRequest(
                "LOSS-ITEM-01",
                "Loss Making Item",
                "Selling below cost is forbidden",
                "Electronics",
                "INR",
                new BigDecimal("100.00"), // Base price 100
                new BigDecimal("150.00"), // Cost price 150 -> base < cost!
                0.10,
                0.10,
                1,
                5,
                10,
                "loss"
        );

        mockMvc.perform(post("/api/v1/merchants/" + merchantId + "/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Domain Rule Violation"))
                .andExpect(jsonPath("$.detail").value("Base price cannot be less than cost price."));
    }
}
