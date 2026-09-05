package io.commercedna.api.config;

import io.commercedna.audit.engine.AuditLedgerEngine;
import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.repository.ProductJpaRepository;
import io.commercedna.core.port.MerchantRepositoryPort;
import io.commercedna.identity.repository.MerchantEntity;
import io.commercedna.identity.repository.MerchantJpaRepository;
import io.commercedna.identity.service.MerchantIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Automatically seeds initial demo data on application startup (Apex Electronics, products, and genesis blocks).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final MerchantJpaRepository merchantRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final ProductJpaRepository productRepository;
    private final AuditLedgerEngine auditLedgerEngine;

    public DataSeeder(
            MerchantJpaRepository merchantRepository,
            MerchantIdentityService merchantIdentityService,
            ProductJpaRepository productRepository,
            AuditLedgerEngine auditLedgerEngine
    ) {
        this.merchantRepository = merchantRepository;
        this.merchantIdentityService = merchantIdentityService;
        this.productRepository = productRepository;
        this.auditLedgerEngine = auditLedgerEngine;
    }

    @Override
    public void run(String... args) {
        if (merchantRepository.findByMerchantCode("apex-tech").isPresent()) {
            return;
        }

        log.info("Initializing CommerceDNA Demonstration Seed Data...");

        // 1. Seed Merchant: Apex Electronics
        MerchantIdentityService.RegisterMerchantCommand cmd = new MerchantIdentityService.RegisterMerchantCommand(
                "apex-tech",
                "Apex Electronics Technologies",
                "sales@apextech.in",
                "rzp_test_apex12345",
                "rzp_sec_apex987654321",
                "whsec_apex_demo_secret_2026"
        );

        MerchantIdentityService.RegistrationResult reg = merchantIdentityService.registerMerchant(cmd);
        log.info("Seeded Merchant: Apex Electronics (DID: {})", reg.merchantDid());

        UUID merchantId = reg.merchant().getId();


        // 2. Seed Products
        ProductEntity p1 = new ProductEntity(
                UUID.randomUUID(),
                merchantId,
                "AURORA-ANC-001",
                "Aurora Wireless ANC Headphones",
                "Premium active noise-cancelling wireless headphones with 40-hour battery life and spatial audio",
                "Audio & Electronics",
                "INR",
                499900L, // Base Price: ₹4,999.00
                250000L, // Cost: ₹2,500.00
                0.15,    // 15% margin floor -> floor = ₹2,875.00
                0.40,    // 40% max discount
                1,
                50,
                120,     // Stock: 120
                "headphones,anc,bluetooth,audio,wireless",
                true,
                Instant.now(),
                Instant.now()
        );

        ProductEntity p2 = new ProductEntity(
                UUID.randomUUID(),
                merchantId,
                "TITAN-OCTANE-CHRONO",
                "Titan Octane Sapphire Chronograph",
                "Water-resistant 100m sports chronograph watch with scratch-resistant sapphire crystal",
                "Watches & Accessories",
                "INR",
                1249900L, // Base Price: ₹12,499.00
                700000L,  // Cost: ₹7,000.00
                0.20,     // 20% margin floor -> floor = ₹8,400.00
                0.35,     // 35% max discount
                1,
                20,
                45,       // Stock: 45
                "watch,chronograph,titan,sapphire,luxury",
                true,
                Instant.now(),
                Instant.now()
        );

        ProductEntity p3 = new ProductEntity(
                UUID.randomUUID(),
                merchantId,
                "NORDIC-STUDIO-MIC",
                "Nordic Studio Condenser Microphone",
                "Cardioid condenser microphone with 192kHz/24bit DAC for podcasting and studio broadcast",
                "Studio Equipment",
                "INR",
                799900L, // Base Price: ₹7,999.00
                420000L, // Cost: ₹4,200.00
                0.18,    // 18% margin floor -> floor = ₹4,956.00
                0.35,
                1,
                30,
                80,      // Stock: 80
                "microphone,studio,podcast,condenser,audio",
                true,
                Instant.now(),
                Instant.now()
        );

        productRepository.save(p1);
        productRepository.save(p2);
        productRepository.save(p3);

        // 3. Record Genesis Audit Ledger Entries
        auditLedgerEngine.recordEvent(
                "MERCHANT_INITIALIZED",
                "MERCHANT",
                merchantId.toString(),
                Map.of("merchantCode", "apex-tech", "name", "Apex Electronics", "status", "VERIFIED")
        );

        auditLedgerEngine.recordEvent(
                "CATALOG_INITIALIZED",
                "CATALOG",
                merchantId.toString(),
                Map.of("itemCount", 3, "status", "MARGIN_GUARDRAILS_ACTIVE")
        );

        log.info("CommerceDNA Demonstration Seed Data initialization complete.");
    }
}
