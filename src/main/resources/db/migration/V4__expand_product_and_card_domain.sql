-- Product and card domain expansion.
-- V1~V3 are intentionally left unchanged. Existing card columns remain during
-- the transition so the application can migrate data without destructive drops.

CREATE TABLE brands (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    logo_url VARCHAR(1000),
    website_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE stores (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(500),
    store_type VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_stores_brand FOREIGN KEY (brand_id) REFERENCES brands (id)
);

CREATE INDEX idx_stores_brand_id ON stores (brand_id);

CREATE TABLE product_collections (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    theme VARCHAR(100),
    production_year INTEGER,
    season VARCHAR(30),
    region VARCHAR(100),
    is_limited BOOLEAN NOT NULL DEFAULT FALSE,
    cover_image_url VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_product_collections_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT uk_product_collections_brand_name UNIQUE (brand_id, name)
);

CREATE INDEX idx_product_collections_brand_id ON product_collections (brand_id);

ALTER TABLE products ADD COLUMN brand_id UUID;
ALTER TABLE products ADD COLUMN product_code VARCHAR(100);
ALTER TABLE products ADD COLUMN offering_type VARCHAR(30) NOT NULL DEFAULT 'PRODUCT';
ALTER TABLE products ADD COLUMN theme VARCHAR(100);
ALTER TABLE products ADD COLUMN production_year INTEGER;
ALTER TABLE products ADD COLUMN origin VARCHAR(100);
ALTER TABLE products ADD COLUMN description TEXT;
ALTER TABLE products ADD COLUMN image_url VARCHAR(1000);
ALTER TABLE products ADD COLUMN warranty_months INTEGER;
ALTER TABLE products ADD COLUMN experience_location VARCHAR(500);
ALTER TABLE products ADD COLUMN available_from TIMESTAMP WITH TIME ZONE;
ALTER TABLE products ADD COLUMN available_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE products ADD COLUMN price DECIMAL(15, 2);
ALTER TABLE products ADD COLUMN is_limited BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE products ADD CONSTRAINT fk_products_brand_new
    FOREIGN KEY (brand_id) REFERENCES brands (id);

ALTER TABLE products ADD CONSTRAINT chk_products_offering_type_new
    CHECK (offering_type IN ('PRODUCT', 'ART', 'GASTRONOMY', 'TRAVEL', 'EVENT', 'OTHER'));

ALTER TABLE products ADD CONSTRAINT chk_products_price_new
    CHECK (price IS NULL OR price >= 0);

ALTER TABLE products ADD CONSTRAINT chk_products_warranty_months_new
    CHECK (warranty_months IS NULL OR warranty_months >= 0);

ALTER TABLE products ADD CONSTRAINT chk_products_available_period_new
    CHECK (available_from IS NULL OR available_until IS NULL OR available_until >= available_from);

CREATE INDEX idx_products_brand_id_new ON products (brand_id);
CREATE INDEX idx_products_offering_type_new ON products (offering_type);

CREATE TABLE product_collection_items (
    id UUID PRIMARY KEY,
    product_collection_id UUID NOT NULL,
    product_id UUID NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_product_collection_items_pair UNIQUE (product_collection_id, product_id),
    CONSTRAINT fk_product_collection_items_collection
        FOREIGN KEY (product_collection_id) REFERENCES product_collections (id),
    CONSTRAINT fk_product_collection_items_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_product_collection_items_display_order CHECK (display_order >= 0)
);

CREATE INDEX idx_product_collection_items_product_id ON product_collection_items (product_id);

CREATE TABLE purchase_qrs (
    id UUID PRIMARY KEY,
    qr_token VARCHAR(255) NOT NULL UNIQUE,
    product_id UUID NOT NULL,
    store_id UUID NOT NULL,
    purchase_date TIMESTAMP WITH TIME ZONE NOT NULL,
    serial_number VARCHAR(255),
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by UUID,
    used_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_purchase_qrs_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_purchase_qrs_store FOREIGN KEY (store_id) REFERENCES stores (id),
    CONSTRAINT fk_purchase_qrs_used_by FOREIGN KEY (used_by) REFERENCES users (id),
    CONSTRAINT chk_purchase_qrs_used_state CHECK (
        (is_used = FALSE AND used_by IS NULL AND used_at IS NULL)
        OR
        (is_used = TRUE AND used_by IS NOT NULL AND used_at IS NOT NULL)
    )
);

CREATE INDEX idx_purchase_qrs_product_id ON purchase_qrs (product_id);
CREATE INDEX idx_purchase_qrs_store_id ON purchase_qrs (store_id);
CREATE INDEX idx_purchase_qrs_used_by ON purchase_qrs (used_by);

CREATE TABLE card_templates (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    front_image_url VARCHAR(1000) NOT NULL,
    back_image_url VARCHAR(1000) NOT NULL,
    allowed_card_type VARCHAR(30),
    resource_data TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_card_templates_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT uk_card_templates_brand_name UNIQUE (brand_id, name),
    CONSTRAINT chk_card_templates_allowed_type
        CHECK (allowed_card_type IS NULL OR allowed_card_type IN ('BASIC', 'COLLECTOR'))
);

CREATE INDEX idx_card_templates_brand_id ON card_templates (brand_id);
CREATE INDEX idx_card_templates_active ON card_templates (is_active);

ALTER TABLE cards ADD COLUMN user_id UUID;
ALTER TABLE cards ADD COLUMN purchase_qr_id UUID;
ALTER TABLE cards ADD COLUMN template_id UUID;
ALTER TABLE cards ADD COLUMN original_card_type VARCHAR(30) NOT NULL DEFAULT 'BASIC';
ALTER TABLE cards ADD COLUMN card_type VARCHAR(30) NOT NULL DEFAULT 'BASIC';
ALTER TABLE cards ADD COLUMN selected_customization_id UUID;
ALTER TABLE cards ADD COLUMN purchase_date TIMESTAMP WITH TIME ZONE;
ALTER TABLE cards ADD COLUMN purchase_store_id UUID;
ALTER TABLE cards ADD COLUMN serial_number VARCHAR(255);
ALTER TABLE cards ADD COLUMN issued_at TIMESTAMP WITH TIME ZONE;

-- Legacy V1 card_token is no longer used for new issuance. Keep existing
-- values for old rows, but allow the new QR-based cards to leave it NULL.
ALTER TABLE cards ALTER COLUMN card_token DROP NOT NULL;

UPDATE cards
SET user_id = owner_id
WHERE owner_id IS NOT NULL;

UPDATE cards
SET purchase_date = purchased_at
WHERE purchased_at IS NOT NULL;

UPDATE cards
SET issued_at = COALESCE(registered_at, created_at)
WHERE issued_at IS NULL;

ALTER TABLE cards ALTER COLUMN status SET DEFAULT 'ACTIVE';

ALTER TABLE cards ADD CONSTRAINT fk_cards_user_new
    FOREIGN KEY (user_id) REFERENCES users (id);
ALTER TABLE cards ADD CONSTRAINT fk_cards_purchase_qr_new
    FOREIGN KEY (purchase_qr_id) REFERENCES purchase_qrs (id);
ALTER TABLE cards ADD CONSTRAINT fk_cards_template_new
    FOREIGN KEY (template_id) REFERENCES card_templates (id);
ALTER TABLE cards ADD CONSTRAINT fk_cards_store_new
    FOREIGN KEY (purchase_store_id) REFERENCES stores (id);
ALTER TABLE cards ADD CONSTRAINT uk_cards_purchase_qr_new UNIQUE (purchase_qr_id);
ALTER TABLE cards ADD CONSTRAINT chk_cards_original_type_new
    CHECK (original_card_type IN ('BASIC', 'COLLECTOR'));
ALTER TABLE cards ADD CONSTRAINT chk_cards_type_new
    CHECK (card_type IN ('BASIC', 'CUSTOMIZE', 'COLLECTOR'));

CREATE INDEX idx_cards_user_id_new ON cards (user_id);
CREATE INDEX idx_cards_purchase_qr_id_new ON cards (purchase_qr_id);
CREATE INDEX idx_cards_selected_customization_id_new ON cards (selected_customization_id);

-- V1 allowed only one customization per card. Keep that table as a legacy
-- backup and create the new 1:N history table without losing old rows.
ALTER TABLE card_customizations RENAME TO card_customizations_legacy;

CREATE TABLE card_customizations (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    template_id UUID,
    input_image_url VARCHAR(1000),
    input_text VARCHAR(1000),
    generated_front_image_url VARCHAR(1000),
    generated_back_image_url VARCHAR(1000),
    generated_message VARCHAR(1000),
    customization_data TEXT,
    ai_model VARCHAR(100),
    generation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_card_customizations_card_new
        FOREIGN KEY (card_id) REFERENCES cards (id),
    CONSTRAINT fk_card_customizations_template_new
        FOREIGN KEY (template_id) REFERENCES card_templates (id),
    CONSTRAINT chk_card_customizations_status_new
        CHECK (generation_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REJECTED', 'ARCHIVED'))
);

CREATE INDEX idx_card_customizations_card_id_new ON card_customizations (card_id);
CREATE INDEX idx_card_customizations_status_new ON card_customizations (generation_status);

INSERT INTO card_customizations (
    id,
    card_id,
    input_image_url,
    input_text,
    generated_front_image_url,
    generated_message,
    generation_status,
    created_at,
    updated_at
)
SELECT
    id,
    card_id,
    NULL,
    NULL,
    image_url,
    message,
    CASE
        WHEN image_url IS NULL AND message IS NULL THEN 'FAILED'
        ELSE 'COMPLETED'
    END,
    created_at,
    updated_at
FROM card_customizations_legacy;

ALTER TABLE cards ADD CONSTRAINT fk_cards_selected_customization_new
    FOREIGN KEY (selected_customization_id) REFERENCES card_customizations (id);
