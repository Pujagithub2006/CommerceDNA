-- =============================================================================
-- CommerceDNA Initial Demonstration Seed Data
-- =============================================================================

INSERT INTO merchants (
    id,
    merchant_code,
    business_name,
    contact_email,
    public_key_ed25519,
    encrypted_private_key_ed25519,
    encrypted_razorpay_key_id,
    encrypted_razorpay_key_secret,
    encrypted_webhook_secret,
    active
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'apex-tech',
    'Apex Electronics Technologies',
    'sales@apextech.in',
    '3q2+796tvu/AW54W2jAEvIJypcgPjaqpdOPITitJY84=',
    'enc:v1:apex_priv_key_seed',
    'enc:v1:rzp_test_apex12345',
    'enc:v1:rzp_sec_apex987654321',
    'enc:v1:whsec_apex_demo_secret_2026',
    true
) ON CONFLICT (merchant_code) DO NOTHING;

INSERT INTO catalog_products (
    id,
    merchant_id,
    sku,
    title,
    description,
    category,
    currency,
    base_price_paise,
    cost_price_paise,
    min_margin_percentage,
    max_discount_percentage,
    min_quantity,
    max_quantity,
    stock_quantity,
    tags,
    active
) VALUES
(
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'AURORA-ANC-001',
    'Aurora Wireless ANC Headphones',
    'Premium active noise-cancelling wireless headphones with 40-hour battery life and spatial audio',
    'Audio & Electronics',
    'INR',
    499900,
    250000,
    0.15,
    0.40,
    1,
    50,
    120,
    'headphones,anc,bluetooth,audio,wireless',
    true
),
(
    'b0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'TITAN-OCTANE-CHRONO',
    'Titan Octane Sapphire Chronograph',
    'Water-resistant 100m sports chronograph watch with scratch-resistant sapphire crystal',
    'Watches & Accessories',
    'INR',
    1249900,
    700000,
    0.20,
    0.35,
    1,
    20,
    45,
    'watch,chronograph,titan,sapphire,luxury',
    true
),
(
    'b0000000-0000-0000-0000-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'NORDIC-STUDIO-MIC',
    'Nordic Studio Condenser Microphone',
    'Cardioid condenser microphone with 192kHz/24bit DAC for podcasting and studio broadcast',
    'Studio Equipment',
    'INR',
    799900,
    420000,
    0.18,
    0.35,
    1,
    30,
    80,
    'microphone,studio,podcast,condenser,audio',
    true
) ON CONFLICT (merchant_id, sku) DO NOTHING;
