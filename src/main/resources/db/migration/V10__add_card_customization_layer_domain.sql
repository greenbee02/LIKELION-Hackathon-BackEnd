-- ============================================================
-- V10: 승인된 카드 에셋 + 커스터마이징 레이어 + 공통 뒷면 레이아웃
-- ============================================================
--
-- BASIC/COLLECTOR 카드는 기존 card_templates의 front_image_url,
-- back_image_url을 그대로 사용한다.
-- CUSTOMIZE 카드는 PRODUCT_BACKGROUND + BORDER + TEXT의 앞면 레이어와
-- 공통 뒷면 레이아웃을 사용한다.


-- ============================================================
-- 1. CARD DESIGN ASSETS
-- 브랜드가 승인한 정적 이미지 에셋 카탈로그
-- ============================================================
CREATE TABLE card_design_assets (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    product_id UUID,
    asset_key VARCHAR(100) NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    name VARCHAR(255) NOT NULL,
    variant_code VARCHAR(30) NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    is_transparent BOOLEAN NOT NULL DEFAULT FALSE,
    width_px INTEGER NOT NULL DEFAULT 1024,
    height_px INTEGER NOT NULL DEFAULT 1536,
    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_card_design_assets_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_card_design_assets_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_card_design_assets_key
        UNIQUE (asset_key),

    CONSTRAINT chk_card_design_assets_type
        CHECK (asset_type IN ('PRODUCT_BACKGROUND', 'BORDER', 'BACK_BASE')),

    CONSTRAINT chk_card_design_assets_product_target
        CHECK (
            (asset_type = 'PRODUCT_BACKGROUND' AND product_id IS NOT NULL)
            OR
            (asset_type IN ('BORDER', 'BACK_BASE') AND product_id IS NULL)
        ),

    CONSTRAINT chk_card_design_assets_transparency
        CHECK (asset_type <> 'BORDER' OR is_transparent = TRUE),

    CONSTRAINT chk_card_design_assets_size
        CHECK (width_px > 0 AND height_px > 0)
);

CREATE INDEX idx_card_design_assets_brand_type
    ON card_design_assets (brand_id, asset_type, is_active);

CREATE INDEX idx_card_design_assets_product
    ON card_design_assets (product_id, asset_type, is_active);


-- ============================================================
-- 2. CARD BACK LAYOUTS
-- 공통 뒷면 배경과 카드 정보의 고정 출력 위치를 관리
-- 실제 값은 CardResponse의 store, purchaseDate, product, serialNumber에서 조회
-- ============================================================
CREATE TABLE card_back_layouts (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    base_asset_id UUID NOT NULL,
    layout_data JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_card_back_layouts_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_card_back_layouts_base_asset
        FOREIGN KEY (base_asset_id)
        REFERENCES card_design_assets (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_card_back_layouts_brand_name
        UNIQUE (brand_id, name),

    CONSTRAINT chk_card_back_layouts_data_object
        CHECK (jsonb_typeof(layout_data) = 'object')
);

CREATE INDEX idx_card_back_layouts_brand_active
    ON card_back_layouts (brand_id, is_active);


-- ============================================================
-- 3. CARD CUSTOMIZATION LAYERS
-- 하나의 커스터마이징 앞면은 상품+배경, 테두리, 문구 레이어 각 1개로 구성
-- 좌표와 크기는 0~1 정규화 좌표를 사용
-- ============================================================
CREATE TABLE card_customization_layers (
    id UUID PRIMARY KEY,
    customization_id UUID NOT NULL,
    asset_id UUID,
    layer_type VARCHAR(30) NOT NULL,
    layer_order INTEGER NOT NULL,
    text_content VARCHAR(1000),
    position_x NUMERIC(8, 6) NOT NULL DEFAULT 0,
    position_y NUMERIC(8, 6) NOT NULL DEFAULT 0,
    width NUMERIC(8, 6) NOT NULL DEFAULT 1,
    height NUMERIC(8, 6) NOT NULL DEFAULT 1,
    rotation NUMERIC(8, 3) NOT NULL DEFAULT 0,
    opacity NUMERIC(7, 6) NOT NULL DEFAULT 1,
    z_index INTEGER NOT NULL DEFAULT 0,
    style_data JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_card_customization_layers_customization
        FOREIGN KEY (customization_id)
        REFERENCES card_customizations (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_card_customization_layers_asset
        FOREIGN KEY (asset_id)
        REFERENCES card_design_assets (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_card_customization_layers_type
        UNIQUE (customization_id, layer_type),

    CONSTRAINT uk_card_customization_layers_order
        UNIQUE (customization_id, layer_order),

    CONSTRAINT chk_card_customization_layers_type
        CHECK (layer_type IN ('PRODUCT_BACKGROUND', 'BORDER', 'TEXT')),

    CONSTRAINT chk_card_customization_layers_content
        CHECK (
            (
                layer_type IN ('PRODUCT_BACKGROUND', 'BORDER')
                AND asset_id IS NOT NULL
                AND text_content IS NULL
            )
            OR
            (
                layer_type = 'TEXT'
                AND asset_id IS NULL
                AND text_content IS NOT NULL
                AND BTRIM(text_content) <> ''
            )
        ),

    CONSTRAINT chk_card_customization_layers_position
        CHECK (
            position_x BETWEEN 0 AND 1
            AND position_y BETWEEN 0 AND 1
            AND width > 0 AND width <= 1
            AND height > 0 AND height <= 1
        ),

    CONSTRAINT chk_card_customization_layers_transform
        CHECK (
            rotation BETWEEN -360 AND 360
            AND opacity BETWEEN 0 AND 1
            AND layer_order >= 0
        ),

    CONSTRAINT chk_card_customization_layers_style_object
        CHECK (jsonb_typeof(style_data) = 'object')
);

CREATE INDEX idx_card_customization_layers_customization
    ON card_customization_layers (customization_id, layer_order);

CREATE INDEX idx_card_customization_layers_asset
    ON card_customization_layers (asset_id);


-- ============================================================
-- 4. CUSTOMIZATION BACK DATA
-- back_content_data는 발급 당시 표시값을 보존하기 위한 선택적 스냅샷
-- ============================================================
ALTER TABLE card_customizations
    ADD COLUMN back_layout_id UUID,
    ADD COLUMN back_content_data JSONB;

ALTER TABLE card_customizations
    ADD CONSTRAINT fk_card_customizations_back_layout
    FOREIGN KEY (back_layout_id)
    REFERENCES card_back_layouts (id)
    ON DELETE RESTRICT;

ALTER TABLE card_customizations
    ADD CONSTRAINT chk_card_customizations_back_content
    CHECK (
        back_content_data IS NULL
        OR jsonb_typeof(back_content_data) = 'object'
    );

CREATE INDEX idx_card_customizations_back_layout
    ON card_customizations (back_layout_id);
