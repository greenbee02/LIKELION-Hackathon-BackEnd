-- ============================================================
-- V14: 기본 카드의 고정 리소스를 공통 앞·뒷면 에셋으로 통일
-- ============================================================
-- 기본 카드 발급 시에는 테두리와 패턴을 AI로 다시 만들지 않는다.
-- 앞면은 상품 이미지 기반 배경 위에 BORDER_03을 사용하고,
-- 뒷면은 상품 구매 정보가 들어갈 공통 검정 레이아웃을 사용한다.

UPDATE card_templates
SET
    front_image_url = '/images/templates/border_03.png',
    back_image_url = '/images/templates/common_back_black_info.png',
    resource_data = (
        COALESCE(NULLIF(resource_data, ''), '{}')::JSONB
        || jsonb_build_object(
            'basicRenderMode', 'PRODUCT_BACKGROUND_WITH_DEFAULT_BORDER',
            'defaultFrontAssetKey', 'BORDER_03',
            'defaultBackAssetKey', 'COMMON_BACK_BLACK_INFO',
            'customization', jsonb_build_object(
                'frontRenderMode', 'THREE_LAYER',
                'frontLayerOrder', jsonb_build_array(
                    'PRODUCT_BACKGROUND',
                    'BORDER',
                    'TEXT'
                ),
                'defaultBorderAssetKey', 'BORDER_03',
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
