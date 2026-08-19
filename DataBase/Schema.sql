-- ============================================================
-- Luxury Collection - MySQL 8.0+
-- ============================================================
-- 참고용 초기 MySQL 스키마다. 현재 애플리케이션 실행·운영 DB 기준은
-- src/main/resources/db/migration/ 의 PostgreSQL Flyway V1~V9이며,
-- 이 파일을 Flyway 또는 운영 DB에 직접 실행하지 않는다.
-- 필요시 DROP 주석제거 후 사용
-- DROP DATABASE IF EXISTS luxury_collection;
CREATE DATABASE luxury_collection
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE luxury_collection;


-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT chk_users_role
        CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN')),

    INDEX idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB;


-- ============================================================
-- 2. SOCIAL ACCOUNTS
-- ============================================================
CREATE TABLE social_accounts (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_social_accounts_provider_user
        UNIQUE (provider, provider_user_id),

    CONSTRAINT fk_social_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    INDEX idx_social_accounts_user_id (user_id)
) ENGINE=InnoDB;


-- ============================================================
-- 3. BRANDS
-- ============================================================
CREATE TABLE brands (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    logo_url VARCHAR(1000) NULL,
    website_url VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_brands_name UNIQUE (name)
) ENGINE=InnoDB;


-- ============================================================
-- 4. STORES
-- ============================================================
CREATE TABLE stores (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address VARCHAR(500) NULL,
    store_type VARCHAR(50) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_stores_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    INDEX idx_stores_brand_id (brand_id),
    INDEX idx_stores_country_city (country, city)
) ENGINE=InnoDB;


-- ============================================================
-- 5. PRODUCT COLLECTIONS
-- ============================================================
CREATE TABLE product_collections (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    theme VARCHAR(100) NULL,
    production_year YEAR NULL,
    season VARCHAR(30) NULL,
    region VARCHAR(100) NULL,
    is_limited BOOLEAN NOT NULL DEFAULT FALSE,
    cover_image_url VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_product_collections_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_product_collections_season
        CHECK (season IS NULL OR season IN ('SS', 'FW', 'CRUISE', 'HOLIDAY', 'ALL_SEASON')),

    CONSTRAINT uk_product_collections_brand_name
        UNIQUE (brand_id, name),

    INDEX idx_product_collections_brand_id (brand_id),
    INDEX idx_product_collections_year_season (production_year, season),
    INDEX idx_product_collections_theme (theme)
) ENGINE=InnoDB;


-- ============================================================
-- 6. PRODUCTS / EXPERIENCES
-- ============================================================
CREATE TABLE products (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,

    product_code VARCHAR(100) NULL,
    name VARCHAR(255) NOT NULL,
    offering_type VARCHAR(30) NOT NULL DEFAULT 'PRODUCT',
    category VARCHAR(100) NULL,
    theme VARCHAR(100) NULL,
    production_year YEAR NULL,
    season VARCHAR(30) NULL,
    region VARCHAR(100) NULL,

    material VARCHAR(100) NULL,
    color VARCHAR(100) NULL,
    origin VARCHAR(100) NULL,

    description TEXT NULL,
    image_url VARCHAR(1000) NULL,

    warranty_info VARCHAR(1000) NULL,
    warranty_months INT NULL,
    care_info TEXT NULL,

    experience_location VARCHAR(500) NULL,
    available_from DATETIME NULL,
    available_until DATETIME NULL,

    price DECIMAL(15, 2) NULL,
    is_limited BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_products_brand_code
        UNIQUE (brand_id, product_code),

    CONSTRAINT chk_products_offering_type
        CHECK (offering_type IN ('PRODUCT', 'ART', 'GASTRONOMY', 'TRAVEL', 'EVENT', 'OTHER')),

    CONSTRAINT chk_products_season
        CHECK (season IS NULL OR season IN ('SS', 'FW', 'CRUISE', 'HOLIDAY', 'ALL_SEASON')),

    CONSTRAINT chk_products_warranty_months
        CHECK (warranty_months IS NULL OR warranty_months >= 0),

    CONSTRAINT chk_products_price
        CHECK (price IS NULL OR price >= 0),

    CONSTRAINT chk_products_available_period
        CHECK (
            available_from IS NULL
            OR available_until IS NULL
            OR available_until >= available_from
        ),

    INDEX idx_products_brand_id (brand_id),
    INDEX idx_products_offering_type (offering_type),
    INDEX idx_products_category (category),
    INDEX idx_products_year_season (production_year, season),
    INDEX idx_products_region (region),
    INDEX idx_products_active (is_active)
) ENGINE=InnoDB;


-- ============================================================
-- 7. PRODUCT COLLECTION ITEMS
-- ============================================================
CREATE TABLE product_collection_items (
    id CHAR(36) PRIMARY KEY,
    product_collection_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_product_collection_items_pair
        UNIQUE (product_collection_id, product_id),

    CONSTRAINT fk_product_collection_items_collection
        FOREIGN KEY (product_collection_id) REFERENCES product_collections(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_product_collection_items_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_product_collection_items_display_order
        CHECK (display_order >= 0),

    INDEX idx_product_collection_items_product_id (product_id)
) ENGINE=InnoDB;


-- ============================================================
-- 8. PURCHASE QRS
-- ============================================================
CREATE TABLE purchase_qrs (
    id CHAR(36) PRIMARY KEY,
    qr_token VARCHAR(255) NOT NULL UNIQUE,
    product_id CHAR(36) NOT NULL,
    store_id CHAR(36) NOT NULL,
    purchase_date DATETIME NOT NULL,
    serial_number VARCHAR(255) NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_by CHAR(36) NULL,
    used_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_purchase_qrs_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_purchase_qrs_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_purchase_qrs_used_by
        FOREIGN KEY (used_by) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_purchase_qrs_used_state
        CHECK (
            (is_used = FALSE AND used_by IS NULL AND used_at IS NULL)
            OR
            (is_used = TRUE AND used_by IS NOT NULL AND used_at IS NOT NULL)
        ),

    INDEX idx_purchase_qrs_product_id (product_id),
    INDEX idx_purchase_qrs_store_id (store_id),
    INDEX idx_purchase_qrs_used_by (used_by),
    INDEX idx_purchase_qrs_purchase_date (purchase_date),
    INDEX idx_purchase_qrs_expires_at (expires_at)
) ENGINE=InnoDB;


-- ============================================================
-- 9. CARD CUSTOMIZATION TEMPLATES
-- ============================================================
CREATE TABLE card_templates (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,

    name VARCHAR(255) NOT NULL,
    description TEXT NULL,

    front_image_url VARCHAR(1000) NOT NULL,
    back_image_url VARCHAR(1000) NOT NULL,
    allowed_card_type VARCHAR(30) NULL,

    resource_data JSON NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_card_templates_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_card_templates_brand_name
        UNIQUE (brand_id, name),

    CONSTRAINT chk_card_templates_allowed_type
        CHECK (allowed_card_type IS NULL OR allowed_card_type IN ('BASIC', 'COLLECTOR')),

    INDEX idx_card_templates_brand_id (brand_id),
    INDEX idx_card_templates_active (is_active)
) ENGINE=InnoDB;


-- ============================================================
-- 10. DIGITAL CARDS
-- ============================================================
CREATE TABLE cards (
    id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    purchase_qr_id CHAR(36) NOT NULL UNIQUE,
    template_id CHAR(36) NOT NULL,

    original_card_type VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    card_type VARCHAR(30) NOT NULL DEFAULT 'BASIC',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    selected_customization_id CHAR(36) NULL,

    purchase_date DATETIME NOT NULL,
    purchase_store_id CHAR(36) NOT NULL,
    serial_number VARCHAR(255) NULL,

    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cards_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cards_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cards_purchase_qr
        FOREIGN KEY (purchase_qr_id) REFERENCES purchase_qrs(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cards_template
        FOREIGN KEY (template_id) REFERENCES card_templates(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_cards_store
        FOREIGN KEY (purchase_store_id) REFERENCES stores(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_cards_type
        CHECK (card_type IN ('BASIC', 'CUSTOMIZE', 'COLLECTOR')),

    CONSTRAINT chk_cards_original_type
        CHECK (original_card_type IN ('BASIC', 'COLLECTOR')),

    CONSTRAINT chk_cards_status
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'REVOKED')),

    INDEX idx_cards_user_id (user_id),
    INDEX idx_cards_product_id (product_id),
    INDEX idx_cards_template_id (template_id),
    INDEX idx_cards_store_id (purchase_store_id),
    INDEX idx_cards_selected_customization_id (selected_customization_id),
    INDEX idx_cards_user_type (user_id, card_type)
) ENGINE=InnoDB;





-- ============================================================
-- 11. CARD CUSTOMIZATIONS
-- ============================================================
CREATE TABLE card_customizations (
    id CHAR(36) PRIMARY KEY,

    card_id CHAR(36) NOT NULL,
    template_id CHAR(36) NOT NULL,

    input_image_url VARCHAR(1000) NULL,
    input_text VARCHAR(1000) NULL,

    generated_front_image_url VARCHAR(1000) NULL,
    generated_back_image_url VARCHAR(1000) NULL,
    generated_message VARCHAR(1000) NULL,

    customization_data JSON NULL,
    ai_model VARCHAR(100) NULL,

    generation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_card_customizations_card
        FOREIGN KEY (card_id) REFERENCES cards(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_card_customizations_template
        FOREIGN KEY (template_id) REFERENCES card_templates(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_card_customizations_status
        CHECK (
            generation_status IN (
                'PENDING',
                'COMPLETED',
                'FAILED',
                'REJECTED',
                'ARCHIVED'
            )
        ),

    INDEX idx_card_customizations_card_id (card_id),
    INDEX idx_card_customizations_template_id (template_id),
    INDEX idx_card_customizations_status (generation_status)
) ENGINE=InnoDB;



-- ============================================================
-- 12. AI RESOURCE GENERATIONS
-- ============================================================
CREATE TABLE ai_resource_generations (
    id CHAR(36) PRIMARY KEY,

    card_id CHAR(36) NOT NULL,
    product_id CHAR(36) NULL,
    template_id CHAR(36) NULL,

    resource_type VARCHAR(30) NOT NULL,
    prompt VARCHAR(2000) NULL,
    source_image_url VARCHAR(1000) NULL,
    generated_image_url VARCHAR(1000) NULL,
    generated_data JSON NULL,
    ai_model VARCHAR(100) NULL,
    generation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(2000) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_resource_generations_card
        FOREIGN KEY (card_id) REFERENCES cards(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_resource_generations_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_ai_resource_generations_template
        FOREIGN KEY (template_id) REFERENCES card_templates(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_ai_resource_generations_type
        CHECK (resource_type IN (
            'BACKGROUND', 'BORDER', 'PATTERN', 'PRODUCT_ANGLE',
            'DECORATION', 'COLOR_PALETTE', 'TEXT_STYLE', 'COMPOSITION'
        )),

    CONSTRAINT chk_ai_resource_generations_status
        CHECK (generation_status IN (
            'PENDING', 'COMPLETED', 'FAILED', 'REJECTED', 'ARCHIVED'
        )),

    INDEX idx_ai_resource_generations_card_id (card_id),
    INDEX idx_ai_resource_generations_product_id (product_id),
    INDEX idx_ai_resource_generations_status (generation_status),
    INDEX idx_ai_resource_generations_type (resource_type)
) ENGINE=InnoDB;



-- ============================================================
-- 13. USER COLLECTIONS
-- ============================================================
CREATE TABLE collections (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    cover_image_url VARCHAR(1000) NULL,
    collection_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    generation_reason TEXT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_collections_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_collections_type
        CHECK (collection_type IN ('CUSTOM', 'AI')),

    INDEX idx_collections_user_id (user_id),
    INDEX idx_collections_user_type (user_id, collection_type)
) ENGINE=InnoDB;


-- ============================================================
-- 13. COLLECTION CARDS
-- ============================================================
CREATE TABLE collection_cards (
    collection_id CHAR(36) NOT NULL,
    card_id CHAR(36) NOT NULL,
    added_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (collection_id, card_id),

    CONSTRAINT fk_collection_cards_collection
        FOREIGN KEY (collection_id) REFERENCES collections(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_cards_card
        FOREIGN KEY (card_id) REFERENCES cards(id)
        ON DELETE CASCADE,

    INDEX idx_collection_cards_card_id (card_id)
) ENGINE=InnoDB;


-- ============================================================
-- 14. REWARDS
-- ============================================================
CREATE TABLE rewards (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    reward_type VARCHAR(50) NOT NULL,
    image_url VARCHAR(1000) NULL,
    quantity INT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rewards_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_rewards_type
        CHECK (reward_type IN ('PHYSICAL_CARD', 'GOODS', 'EVENT_INVITATION', 'BENEFIT')),

    CONSTRAINT chk_rewards_quantity
        CHECK (quantity IS NULL OR quantity >= 0),

    INDEX idx_rewards_brand_id (brand_id),
    INDEX idx_rewards_active (is_active)
) ENGINE=InnoDB;


-- ============================================================
-- 15. EVENTS
-- ============================================================
CREATE TABLE events (
    id CHAR(36) PRIMARY KEY,
    brand_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    location VARCHAR(500) NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    capacity INT NULL,
    image_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_brand
        FOREIGN KEY (brand_id) REFERENCES brands(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_events_period CHECK (end_at >= start_at),
    CONSTRAINT chk_events_capacity CHECK (capacity IS NULL OR capacity >= 0),

    INDEX idx_events_brand_id (brand_id),
    INDEX idx_events_start_at (start_at)
) ENGINE=InnoDB;


-- ============================================================
-- 16. COLLECTION REWARD CONDITIONS
-- ============================================================
CREATE TABLE collection_rewards (
    id CHAR(36) PRIMARY KEY,
    product_collection_id CHAR(36) NOT NULL,
    reward_id CHAR(36) NULL,
    event_id CHAR(36) NULL,
    required_percentage DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_collection_rewards_product_collection
        FOREIGN KEY (product_collection_id) REFERENCES product_collections(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_rewards_reward
        FOREIGN KEY (reward_id) REFERENCES rewards(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_rewards_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_collection_rewards_target
        CHECK (
            (reward_id IS NOT NULL AND event_id IS NULL)
            OR
            (reward_id IS NULL AND event_id IS NOT NULL)
        ),

    CONSTRAINT chk_collection_rewards_percentage
        CHECK (required_percentage > 0 AND required_percentage <= 100),

    INDEX idx_collection_rewards_collection (product_collection_id)
) ENGINE=InnoDB;


-- ============================================================
-- 17. USER REWARD UNLOCK / CLAIM
-- ============================================================
CREATE TABLE user_rewards (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    reward_id CHAR(36) NULL,
    event_id CHAR(36) NULL,
    unlocked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'UNLOCKED',
    claim_code VARCHAR(100) NULL UNIQUE,
    claimed_at DATETIME NULL,
    expires_at DATETIME NULL,

    CONSTRAINT fk_user_rewards_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_rewards_reward
        FOREIGN KEY (reward_id) REFERENCES rewards(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_rewards_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_user_rewards_target
        CHECK (
            (reward_id IS NOT NULL AND event_id IS NULL)
            OR
            (reward_id IS NULL AND event_id IS NOT NULL)
        ),

    CONSTRAINT chk_user_rewards_status
        CHECK (status IN ('UNLOCKED', 'CLAIMED', 'EXPIRED', 'CANCELLED')),

    INDEX idx_user_rewards_user_id (user_id),
    INDEX idx_user_rewards_status (status)
) ENGINE=InnoDB;


-- ============================================================
-- 18. PHYSICAL CARDS
-- ============================================================
CREATE TABLE physical_cards (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    digital_card_id CHAR(36) NULL,
    user_reward_id CHAR(36) NULL,
    physical_card_type VARCHAR(30) NOT NULL,
    physical_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at DATETIME NULL,
    used_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_physical_cards_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_physical_cards_digital_card
        FOREIGN KEY (digital_card_id) REFERENCES cards(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_physical_cards_user_reward
        FOREIGN KEY (user_reward_id) REFERENCES user_rewards(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_physical_cards_type
        CHECK (physical_card_type IN ('PURCHASE_CARD', 'REWARD_PASS')),

    CONSTRAINT chk_physical_cards_source
        CHECK (
            (physical_card_type = 'PURCHASE_CARD' AND digital_card_id IS NOT NULL AND user_reward_id IS NULL)
            OR
            (physical_card_type = 'REWARD_PASS' AND digital_card_id IS NULL AND user_reward_id IS NOT NULL)
        ),

    CONSTRAINT chk_physical_cards_status
        CHECK (status IN ('ISSUED', 'ACTIVE', 'USED', 'EXPIRED', 'BLOCKED')),

    CONSTRAINT uk_physical_cards_digital_card UNIQUE (digital_card_id),
    CONSTRAINT uk_physical_cards_user_reward UNIQUE (user_reward_id),

    INDEX idx_physical_cards_user_id (user_id),
    INDEX idx_physical_cards_status (status),
    INDEX idx_physical_cards_expires_at (expires_at)
) ENGINE=InnoDB;


-- ============================================================
-- 19. AI COLLECTION ANALYSIS
-- ============================================================
CREATE TABLE ai_collection_analyses (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    source_collection_id CHAR(36) NULL,
    generated_collection_id CHAR(36) NULL,
    analysis_type VARCHAR(50) NOT NULL,
    input_snapshot JSON NULL,
    result JSON NOT NULL,
    summary TEXT NULL,
    ai_model VARCHAR(100) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_collection_analyses_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_collection_analyses_source_collection
        FOREIGN KEY (source_collection_id) REFERENCES collections(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_collection_analyses_generated_collection
        FOREIGN KEY (generated_collection_id) REFERENCES collections(id)
        ON DELETE RESTRICT,

    INDEX idx_ai_collection_analyses_user_id (user_id),
    INDEX idx_ai_collection_analyses_source_collection (source_collection_id),
    INDEX idx_ai_collection_analyses_generated_collection (generated_collection_id)
) ENGINE=InnoDB;


-- ============================================================
-- 20. AI COLLECTION RECOMMENDATIONS
-- ============================================================
CREATE TABLE ai_collection_recommendations (
    id CHAR(36) PRIMARY KEY,
    analysis_id CHAR(36) NOT NULL,
    product_id CHAR(36) NOT NULL,
    reason TEXT NULL,
    priority INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_collection_recommendations_analysis
        FOREIGN KEY (analysis_id) REFERENCES ai_collection_analyses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_collection_recommendations_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_ai_collection_recommendations_item
        UNIQUE (analysis_id, product_id),

    CONSTRAINT chk_ai_collection_recommendations_priority
        CHECK (priority >= 0),

    INDEX idx_ai_collection_recommendations_product (product_id)
) ENGINE=InnoDB;


-- ============================================================
-- 21. AI EXPERIENCE RECOMMENDATIONS
-- ============================================================
CREATE TABLE ai_experience_recommendations (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    analysis_id CHAR(36) NULL,
    product_id CHAR(36) NULL,
    store_id CHAR(36) NULL,
    event_id CHAR(36) NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    reason TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECOMMENDED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_experience_recommendations_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_experience_recommendations_analysis
        FOREIGN KEY (analysis_id) REFERENCES ai_collection_analyses(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_store
        FOREIGN KEY (store_id) REFERENCES stores(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_event
        FOREIGN KEY (event_id) REFERENCES events(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ai_experience_recommendations_target
        CHECK (product_id IS NOT NULL OR store_id IS NOT NULL OR event_id IS NOT NULL),

    CONSTRAINT chk_ai_experience_recommendation_status
        CHECK (status IN ('RECOMMENDED', 'VIEWED', 'ACCEPTED', 'DISMISSED')),

    INDEX idx_ai_experience_recommendations_user (user_id),
    INDEX idx_ai_experience_recommendations_analysis (analysis_id),
    INDEX idx_ai_experience_recommendations_status (status)
) ENGINE=InnoDB;

ALTER TABLE cards
    ADD CONSTRAINT fk_cards_selected_customization
        FOREIGN KEY (selected_customization_id)
        REFERENCES card_customizations(id)
        ON DELETE SET NULL;
