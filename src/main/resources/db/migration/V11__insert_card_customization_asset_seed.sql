-- ============================================================
-- V11: 카드 커스터마이징 정적 에셋 및 공통 뒷면 레이아웃 시드
-- ============================================================
-- 이미지 기준 경로:
-- src/main/resources/static/images/templates


-- ============================================================
-- 1. PRODUCT + BACKGROUND ASSETS
-- 11개 상품마다 A/B/C 배경 후보 3개
-- ============================================================
WITH product_seed(product_id, product_no) AS (
    VALUES
        ('50000000-0000-0000-0000-000000000001'::UUID, '001'),
        ('50000000-0000-0000-0000-000000000002'::UUID, '002'),
        ('50000000-0000-0000-0000-000000000003'::UUID, '003'),
        ('50000000-0000-0000-0000-000000000004'::UUID, '004'),
        ('50000000-0000-0000-0000-000000000005'::UUID, '005'),
        ('50000000-0000-0000-0000-000000000006'::UUID, '006'),
        ('50000000-0000-0000-0000-000000000007'::UUID, '007'),
        ('50000000-0000-0000-0000-000000000008'::UUID, '008'),
        ('50000000-0000-0000-0000-000000000009'::UUID, '009'),
        ('50000000-0000-0000-0000-000000000010'::UUID, '010'),
        ('50000000-0000-0000-0000-000000000011'::UUID, '011')
),
variant_seed(variant_code, variant_order) AS (
    VALUES ('A', 1), ('B', 2), ('C', 3)
),
asset_seed AS (
    SELECT
        product_id,
        product_no,
        variant_code,
        ((product_no::INTEGER - 1) * 3 + variant_order) AS asset_sequence
    FROM product_seed
    CROSS JOIN variant_seed
)
INSERT INTO card_design_assets (
    id,
    brand_id,
    product_id,
    asset_key,
    asset_type,
    name,
    variant_code,
    image_url,
    is_transparent,
    width_px,
    height_px,
    metadata,
    is_active,
    created_at,
    updated_at
)
SELECT
    (
        'a1000000-0000-0000-0000-'
        || LPAD(asset_sequence::TEXT, 12, '0')
    )::UUID,
    '20000000-0000-0000-0000-000000000001'::UUID,
    product_id,
    'PROD_' || product_no || '_' || variant_code,
    'PRODUCT_BACKGROUND',
    'Product ' || product_no || ' Background ' || variant_code,
    variant_code,
    '/images/templates/prod_' || product_no || '_' || variant_code || '.png',
    FALSE,
    1024,
    1536,
    jsonb_build_object(
        'layerRole', 'PRODUCT_BACKGROUND',
        'productNumber', product_no,
        'backgroundVariant', variant_code,
        'recommendedZIndex', 10
    ),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM asset_seed;


-- ============================================================
-- 2. BORDER ASSETS
-- BORDER 파일은 실제 알파 채널이 있는 투명 PNG여야 한다.
-- ============================================================
INSERT INTO card_design_assets (
    id,
    brand_id,
    product_id,
    asset_key,
    asset_type,
    name,
    variant_code,
    image_url,
    is_transparent,
    width_px,
    height_px,
    metadata,
    is_active,
    created_at,
    updated_at
) VALUES
(
    'a1000000-0000-0000-0000-000000000034',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'BORDER_01',
    'BORDER',
    'Champagne Gold Border 01',
    '01',
    '/images/templates/border_01.png',
    TRUE,
    1024,
    1536,
    jsonb_build_object('layerRole', 'BORDER', 'recommendedZIndex', 20),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a1000000-0000-0000-0000-000000000035',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'BORDER_02',
    'BORDER',
    'Champagne Gold Border 02',
    '02',
    '/images/templates/border_02.png',
    TRUE,
    1024,
    1536,
    jsonb_build_object('layerRole', 'BORDER', 'recommendedZIndex', 20),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a1000000-0000-0000-0000-000000000036',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'BORDER_03',
    'BORDER',
    'Champagne Gold Border 03',
    '03',
    '/images/templates/border_03.png',
    TRUE,
    1024,
    1536,
    jsonb_build_object('layerRole', 'BORDER', 'recommendedZIndex', 20),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 3. COMMON BACK BASE ASSET
-- ============================================================
INSERT INTO card_design_assets (
    id,
    brand_id,
    product_id,
    asset_key,
    asset_type,
    name,
    variant_code,
    image_url,
    is_transparent,
    width_px,
    height_px,
    metadata,
    is_active,
    created_at,
    updated_at
) VALUES (
    'a1000000-0000-0000-0000-000000000037',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'COMMON_BACK_BLACK_INFO',
    'BACK_BASE',
    'MCM Common Black Information Back',
    'BLACK_INFO',
    '/images/templates/common_back_black_info.png',
    FALSE,
    1024,
    1536,
    jsonb_build_object(
        'layerRole', 'BACK_BASE',
        'containsStaticHeader', TRUE,
        'containsStaticFooter', TRUE,
        'informationArea', 'CENTER'
    ),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. COMMON BACK FIELD LAYOUT
-- 좌표는 1024x1536 캔버스를 기준으로 한 0~1 정규화 값
-- ============================================================
INSERT INTO card_back_layouts (
    id,
    brand_id,
    name,
    base_asset_id,
    layout_data,
    is_active,
    created_at,
    updated_at
) VALUES (
    'a2000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'MCM Common Black Purchase Record',
    'a1000000-0000-0000-0000-000000000037',
    jsonb_build_object(
        'version', 1,
        'coordinateSystem', 'NORMALIZED',
        'canvas', jsonb_build_object('width', 1024, 'height', 1536),
        'baseAssetKey', 'COMMON_BACK_BLACK_INFO',
        'safeArea', jsonb_build_object(
            'left', 0.10,
            'right', 0.90,
            'top', 0.22,
            'bottom', 0.77
        ),
        'labelStyle', jsonb_build_object(
            'fontFamily', 'Pretendard, Arial, sans-serif',
            'fontSize', 24,
            'fontWeight', 400,
            'letterSpacing', 1.2,
            'color', '#B8AA99',
            'textAlign', 'LEFT'
        ),
        'valueStyle', jsonb_build_object(
            'fontFamily', 'Pretendard, Arial, sans-serif',
            'fontSize', 28,
            'fontWeight', 400,
            'lineHeight', 1.35,
            'color', '#D8CEC1',
            'textAlign', 'LEFT'
        ),
        'fields', jsonb_build_array(
            jsonb_build_object(
                'key', 'STORE',
                'label', 'STORE',
                'source', 'store.name',
                'labelX', 0.10,
                'valueX', 0.29,
                'y', 0.25,
                'width', 0.61,
                'maxLines', 2
            ),
            jsonb_build_object(
                'key', 'DATE',
                'label', 'DATE',
                'source', 'purchaseDate',
                'format', 'yyyy.MM.dd',
                'labelX', 0.10,
                'valueX', 0.29,
                'y', 0.36,
                'width', 0.61,
                'maxLines', 1
            ),
            jsonb_build_object(
                'key', 'LOCATION',
                'label', 'LOCATION',
                'source', 'store.city,store.country',
                'format', '{city}, {country}',
                'labelX', 0.10,
                'valueX', 0.29,
                'y', 0.44,
                'width', 0.61,
                'maxLines', 2
            ),
            jsonb_build_object(
                'key', 'PRODUCT',
                'label', 'PRODUCT',
                'source', 'product.name',
                'labelX', 0.10,
                'valueX', 0.29,
                'y', 0.53,
                'width', 0.61,
                'maxLines', 2
            ),
            jsonb_build_object(
                'key', 'SERIAL_NUMBER',
                'label', 'SERIAL NUMBER',
                'source', 'serialNumber',
                'labelX', 0.10,
                'valueX', 0.29,
                'y', 0.64,
                'width', 0.61,
                'maxLines', 1
            )
        )
    ),
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. EXISTING BASIC TEMPLATE METADATA UPDATE
-- 기본 카드는 기존 고정 앞/뒷면을 유지하고, 커스터마이징 시 사용할 구조만 안내
-- ============================================================
UPDATE card_templates
SET
    resource_data = (
        COALESCE(NULLIF(resource_data, ''), '{}')::JSONB
        || jsonb_build_object(
            'basicRenderMode', 'FIXED_IMAGE_PAIR',
            'customization', jsonb_build_object(
                'frontRenderMode', 'THREE_LAYER',
                'frontLayerOrder', jsonb_build_array(
                    'PRODUCT_BACKGROUND',
                    'BORDER',
                    'TEXT'
                ),
                'backRenderMode', 'COMMON_LAYOUT',
                'backLayoutId', 'a2000000-0000-0000-0000-000000000001'
            )
        )
    )::TEXT,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    '60000000-0000-0000-0000-000000000001',
    '60000000-0000-0000-0000-000000000002',
    '60000000-0000-0000-0000-000000000003'
);
