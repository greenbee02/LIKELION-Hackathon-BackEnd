-- ============================================================
-- 시드 데이터
-- ============================================================
-- ============================================================
-- 1. USERS
-- ============================================================

INSERT INTO users (
    id,
    email,
    password_hash,
    name,
    role,
    created_at,
    updated_at
) VALUES
(
    '10000000-0000-0000-0000-000000000001',
    'customer@example.com',
    NULL,
    'Test Customer',
    'CUSTOMER',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. BRANDS
-- ============================================================

INSERT INTO brands (
    id,
    name,
    description,
    logo_url,
    website_url,
    created_at,
    updated_at
) VALUES
(
    '20000000-0000-0000-0000-000000000001',
    'MCM',
    'Modern Creation München',
    '/images/brands/mcm.png',
    'https://kr.mcmworldwide.com/ko_KR/home?srsltid=AfmBOoq8P08TgiDPkiBKcIaNAdHGl6PaaEcbA7g3DI-oXG8kWBwYsGvP',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 3. STORES
-- ============================================================

INSERT INTO stores (
    id,
    brand_id,
    name,
    country,
    city,
    address,
    store_type,
    created_at,
    updated_at
) VALUES
(
    '30000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'MCM 신세계면세점 본점',
    'South Korea',
    'Seoul',
    '서울특별시 중구 퇴계로 77 신세계백화점 본점 9F',
    'Head Store',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '30000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    'Ikebukuro Tobu',
    'Japan',
    'Tokyo',
    '1-1-25 Nishi-ikebukuro,Toshima-ku, Tokyo,Japan, Toshima-ku 1718512',
    'Head Store',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '30000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'MCMNEW YORK SOHO',
    'United States',
    'New York',
    '100 Greene Street, New York, NY',
    'Flagship',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. PRODUCT COLLECTIONS
-- 브랜드가 정의한 공식 컬렉션
-- ============================================================

INSERT INTO product_collections (
    id,
    brand_id,
    name,
    description,
    theme,
    production_year,
    season,
    region,
    is_limited,
    created_at,
    updated_at
) VALUES
(
    '40000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'Seoul Exclusive',
    '서울 매장에서 만날 수 있는 지역 한정 상품과 경험을 모은 컬렉션',
    'REGIONAL',
    2026,
    'ALL_SEASON',
    'Seoul',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '40000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    '2026 New Arrivals',
    '2026년에 새롭게 출시된 주요 신상품을 모은 컬렉션',
    'NEW_ARRIVAL',
    2026,
    'ALL_SEASON',
    NULL,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '40000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'Women''s Signature',
    '브랜드의 대표적인 여성 가방과 액세서리를 중심으로 구성한 컬렉션',
    'WOMEN',
    NULL,
    'ALL_SEASON',
    NULL,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '40000000-0000-0000-0000-000000000004',
    '20000000-0000-0000-0000-000000000001',
    'Global Travel Collection',
    '여행에 적합한 캐리어, 백팩, 보스턴백과 라이프스타일 제품을 모은 컬렉션',
    'TRAVEL',
    NULL,
    'ALL_SEASON',
    NULL,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '40000000-0000-0000-0000-000000000005',
    '20000000-0000-0000-0000-000000000001',
    'MCM Icons',
    '오랫동안 브랜드를 대표해 온 아이코닉 상품을 모은 시그니처 컬렉션',
    'ICONIC',
    NULL,
    'ALL_SEASON',
    NULL,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================
-- 5. PRODUCTS / EXPERIENCES
-- ============================================================


INSERT INTO products (
    id,
    brand_id,
    product_code,
    name,
    offering_type,
    category,
    theme,
    production_year,
    season,
    region,
    material,
    color,
    description,
    image_url,
    care_info,
    price,
    is_limited,
    is_active,
    created_at,
    updated_at
) VALUES
(
    '50000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    '실크 파자마 셔츠',
    'PRODUCT',
    'SHIRT',
    'NEW_ARRIVAL',
    2026,
    'FW',
    NULL,
    'Silk',
    'Orangeade',
    '그래픽 프린트가 더해진 여성용 긴소매 실크 파자마 셔츠',
    '/images/products/prod_001.png',
    '드라이클리닝 권장. 표백 및 건조기 사용 금지.',
    1050000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    '디스코 모노그램 프린트 쁘띠 스카프',
    'PRODUCT',
    'SCARF',
    'NEW_ARRIVAL',
    2026,
    'FW',
    NULL,
    'Organic Silk',
    'Aw26 Sangria Sunset',
    'MCM Disco 아트워크와 손바느질 마감이 적용된 양면 오가닉 실크 스카프',
    '/images/products/prod_002.png',
    '드라이클리닝 권장.',
    175000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'Rockstar 골드 크리스탈 비세토스 베니티 케이스',
    'PRODUCT',
    'VANITY_CASE',
    'WOMEN',
    NULL,
    'ALL_SEASON',
    NULL,
    'Visetos Canvas, Leather, Crystal',
    'Cognac',
    '가죽 탑 핸들과 크로스바디 스트랩으로 두 가지 스타일을 연출할 수 있는 베니티 케이스',
    '/images/products/prod_003.png',
    '더스트백에 넣어 서늘하고 건조한 곳에 보관하고 물기와 거친 표면을 피하세요.',
    1550000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000004',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    '코스믹 스타 오 드 퍼퓸',
    'PRODUCT',
    'PERFUME',
    'WOMEN',
    NULL,
    'ALL_SEASON',
    NULL,
    NULL,
    'White',
    '자신만의 빛으로 세상을 밝히는 이들을 위한 향수',
    '/images/products/prod_004.png',
    '외용 전용. 알코올을 함유하고 있으므로 화기 가까이에서 사용하지 마세요.',
    141000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000005',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'Aren 다이아몬드 퀼팅 레더 백팩',
    'PRODUCT',
    'BACKPACK',
    'TRAVEL',
    NULL,
    'ALL_SEASON',
    NULL,
    'Quilted Nappa Leather',
    'Black',
    '다이아몬드 모티프 퀼팅이 적용된 나파 가죽 백팩',
    '/images/products/prod_005.png',
    '더스트백에 넣어 서늘하고 건조한 곳에 보관하고 물기와 거친 표면을 피하세요.',
    2690000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000006',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    '비세토스 수트케이스',
    'PRODUCT',
    'SUITCASE',
    'TRAVEL',
    NULL,
    'ALL_SEASON',
    NULL,
    'Visetos Monogram Canvas',
    'Cognac',
    '브랜드의 시대를 초월한 캐리어 수공예 기술을 보여주는 비세토스 수트케이스',
    '/images/products/prod_006.png',
    '서늘하고 건조한 곳에 보관하고 물기, 오염 및 거친 표면을 피하세요.',
    6750000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000007',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'Ottomar 비세토스 여권 케이스',
    'PRODUCT',
    'PASSPORT_CASE',
    'TRAVEL',
    NULL,
    'ALL_SEASON',
    NULL,
    'Visetos Monogram Canvas, Natural Leather',
    'Cinnamon',
    '천연 가죽 트림이 돋보이는 비세토스 모노그램 캔버스 여권 케이스',
    '/images/products/prod_007.png',
    '더스트백에 넣어 서늘하고 건조한 곳에 보관하고 물기와 오염을 피하세요.',
    430000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000008',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'MCM Park 비세토스 토끼 인형',
    'PRODUCT',
    'LIFESTYLE',
    'LIFESTYLE',
    NULL,
    'ALL_SEASON',
    'Seoul',
    'Visetos Monogram Canvas, Faux Fur, Natural Leather',
    'Silver',
    '페이크퍼와 천연 가죽 트림이 더해진 비세토스 모노그램 캔버스 토끼 인형',
    '/images/products/prod_008.png',
    '서늘하고 건조한 곳에 보관하고 물기, 오염 및 거친 표면을 피하세요.',
    750000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000009',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    '모노그램 프린트 양가죽 크로스 샌들',
    'PRODUCT',
    'SANDALS',
    'WOMEN',
    NULL,
    'ALL_SEASON',
    NULL,
    'Lamb Leather',
    'Cognac',
    '비세토스 모노그램 프린트와 라우렐 로고 장식이 적용된 유니섹스 양가죽 슬라이드',
    '/images/products/prod_009.png',
    '밑창은 부드러운 브러시로, 어퍼는 살짝 젖은 부드러운 천으로 관리하세요.',
    830000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000010',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'New Liz 엠보스드 모노그램 레더 쇼퍼',
    'PRODUCT',
    'SHOPPER_BAG',
    'ICONIC',
    NULL,
    'ALL_SEASON',
    NULL,
    'Grained Nappa Leather',
    'Black',
    '엠보싱 비세토스 모노그램과 탈착 가능한 지퍼 파우치가 포함된 나파 가죽 쇼퍼백',
    '/images/products/prod_010.png',
    '더스트백에 넣어 서늘하고 건조한 곳에 보관하고 물기와 거친 표면을 피하세요.',
    1490000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '50000000-0000-0000-0000-000000000011',
    '20000000-0000-0000-0000-000000000001',
    NULL,
    'Ella 비세토스 보스턴 백',
    'PRODUCT',
    'BOSTON_BAG',
    'ICONIC',
    NULL,
    'ALL_SEASON',
    NULL,
    'Visetos Monogram Canvas, Natural Leather',
    'Cognac',
    '천연 가죽 트림과 로고 엠보싱 가죽 참이 더해진 비세토스 모노그램 캔버스 보스턴 백',
    '/images/products/prod_011.png',
    '더스트백에 넣어 서늘하고 건조한 곳에 보관하고 물기와 거친 표면을 피하세요.',
    1250000,
    FALSE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- 상품이나 경험을 추가할 때 위 VALUES 묶음을 쉼표로 연결
-- (
--     '50000000-0000-0000-0000-000000000002',
--     ...
-- );


-- ============================================================
-- 6. PRODUCT COLLECTION ITEMS
-- 상품과 공식 컬렉션 연결
-- ============================================================

INSERT INTO product_collection_items (
    id,
    product_collection_id,
    product_id,
    display_order,
    is_required,
    created_at,
    updated_at
) VALUES

-- Seoul Exclusive: 시연용 임의 구성
(
    '41000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000003',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000004',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000008',
    3,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- 2026 New Arrivals
(
    '41000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000001',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000005',
    '40000000-0000-0000-0000-000000000002',
    '50000000-0000-0000-0000-000000000002',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Women's Signature
(
    '41000000-0000-0000-0000-000000000006',
    '40000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000001',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000007',
    '40000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000002',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000008',
    '40000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000003',
    3,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000009',
    '40000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000004',
    4,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000010',
    '40000000-0000-0000-0000-000000000003',
    '50000000-0000-0000-0000-000000000009',
    5,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Global Travel Collection
(
    '41000000-0000-0000-0000-000000000011',
    '40000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000005',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000012',
    '40000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000006',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000013',
    '40000000-0000-0000-0000-000000000004',
    '50000000-0000-0000-0000-000000000007',
    3,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- MCM Icons
(
    '41000000-0000-0000-0000-000000000014',
    '40000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000010',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '41000000-0000-0000-0000-000000000015',
    '40000000-0000-0000-0000-000000000005',
    '50000000-0000-0000-0000-000000000011',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 7. CARD TEMPLATES
-- ============================================================

INSERT INTO card_templates (
    id,
    brand_id,
    name,
    description,
    front_image_url,
    back_image_url,
    resource_data,
    is_active,
    created_at,
    updated_at
) VALUES
(
    '60000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'MCM Classic Visetos',
    '클래식 비세토스 패턴과 블랙·코냑 색상을 활용한 카드 템플릿',
    '/images/templates/template_001_front.png',
    '/images/templates/template_001_back.png',
    jsonb_build_object(
        'primaryColor', '#15120F',
        'secondaryColor', '#8B6B45',
        'textColor', '#E8DFD2',
        'accentColor', '#B89A6A',
        'fontStyle', 'CLASSIC_SERIF',
        'pattern', 'VISETOS_MONOGRAM',
        'frontLayout', 'PRODUCT_HERO',
        'backLayout', 'PURCHASE_RECORD',
        'graphicStyle', 'QUIET_LUXURY'
    )::TEXT,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '60000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    'AW26 Sangria Sunset',
    'AW26 상그리아 색상과 절제된 디스코 그래픽을 활용한 시즌 카드 템플릿',
    '/images/templates/template_002_front.png',
    '/images/templates/template_002_back.png',
    jsonb_build_object(
        'primaryColor', '#1B1218',
        'secondaryColor', '#6E1F38',
        'textColor', '#F2E7DA',
        'accentColor', '#C4A16B',
        'fontStyle', 'MODERN_SANS',
        'pattern', 'DISCO_MONOGRAM',
        'frontLayout', 'PRODUCT_HERO',
        'backLayout', 'PURCHASE_RECORD',
        'graphicStyle', 'SEASONAL_LUXURY'
    )::TEXT,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '60000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'Seoul Night Edition',
    '서울의 야경과 남산 타워를 배경으로 구성한 지역 테마 카드 템플릿',
    '/images/templates/template_003_front.png',
    '/images/templates/template_003_back.png',
    jsonb_build_object(
        'primaryColor', '#07111D',
        'secondaryColor', '#101E2D',
        'textColor', '#E6E1D9',
        'accentColor', '#B8B4AA',
        'fontStyle', 'ELEGANT_SERIF',
        'pattern', 'SEOUL_NIGHT',
        'frontLayout', 'CITY_PRODUCT_HERO',
        'backLayout', 'PURCHASE_RECORD',
        'graphicStyle', 'REGIONAL_LUXURY'
    )::TEXT,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 8. REWARDS
-- ============================================================

INSERT INTO rewards (
    id,
    brand_id,
    name,
    description,
    reward_type,
    image_url,
    quantity, -- 발급 가능 최대 개수
    is_active,
    expires_at
) VALUES
(
    '70000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    'Seoul Collector Pass',
    'Seoul Exclusive 컬렉션 완성 고객에게 발급되는 한정 실물 카드. 서울 브랜드 행사 입장권으로 사용할 수 있습니다.',
    'PHYSICAL_CARD',
    NULL,
    100,
    TRUE,
    '2027-12-31 23:59:59'
),
(
    '70000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    'AW26 Limited Card Holder',
    '2026 New Arrivals 컬렉션 완성 고객에게 제공되는 비매품 한정 카드 홀더.',
    'GOODS',
    NULL,
    50,
    TRUE,
    '2027-06-30 23:59:59'
),
(
    '70000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'MCM Icons Premium Care',
    'MCM Icons 컬렉션 완성 고객에게 제공되는 공식 제품 점검 및 프리미엄 케어 혜택.',
    'BENEFIT',
    NULL,
    NULL,
    TRUE,
    '2027-12-31 23:59:59'
);


-- ============================================================
-- 9. EVENTS
-- ============================================================

-- ============================================================
-- EVENTS
-- 시연을 위해 모두 현재 진행 중인 일정으로 설정
-- ============================================================

INSERT INTO events (
    id,
    brand_id,
    name,
    description,
    location,
    start_at,
    end_at,
    capacity,
    image_url,
    is_active
) VALUES
(
    '80000000-0000-0000-0000-000000000001',
    '20000000-0000-0000-0000-000000000001',
    '롯데백화점 본점 MCM 스페셜 프로모션',
    '백화점 본매장에서 진행되는 할인 및 구매 금액별 하트 참 증정 프로모션입니다.',
    '롯데백화점 본점 MCM 매장, Seoul',
    '2026-08-01 10:30:00',
    '2026-12-31 20:00:00',
    NULL,
    NULL,
    TRUE
),
(
    '80000000-0000-0000-0000-000000000002',
    '20000000-0000-0000-0000-000000000001',
    '밀라노 디자인 위크 2026 Disco on Mars',
    'MCM 50주년을 기념하여 우주적 콘셉트로 구성한 몰입형 디자인 전시입니다.',
    'Rotonda del Pellegrini, Milan, Italy',
    '2026-07-01 10:00:00',
    '2026-10-31 20:00:00',
    NULL,
    NULL,
    TRUE
),
(
    '80000000-0000-0000-0000-000000000003',
    '20000000-0000-0000-0000-000000000001',
    'MCM HAUS From Munich to Mars 특별전',
    '아티스트 케빈 박과 협업하여 MCM 브랜드의 50년 여정을 다루는 특별전입니다.',
    'MCM HAUS Cheongdam, Seoul',
    '2026-08-15 11:00:00',
    '2026-11-30 20:00:00',
    NULL,
    NULL,
    TRUE
);

-- ============================================================
-- 10. COLLECTION REWARD CONDITIONS
-- ============================================================

-- ============================================================
-- COLLECTION REWARD CONDITIONS
--
-- 컬렉션 ID
-- 001: Seoul Exclusive
-- 002: 2026 New Arrivals
-- 003: Women's Signature
-- 004: Global Travel Collection
-- 005: MCM Icons
-- ============================================================

INSERT INTO collection_rewards (
    id,
    product_collection_id,
    reward_id,
    event_id,
    required_percentage
) VALUES

-- Seoul Exclusive 66.67%
-- 상품 3개 중 2개 보유 시 롯데백화점 프로모션 해금
(
    '90000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    NULL,
    '80000000-0000-0000-0000-000000000001',
    66.67
),

-- Seoul Exclusive 100%
-- 컬렉션 완성 시 실물 Collector Pass 지급
(
    '90000000-0000-0000-0000-000000000002',
    '40000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000001',
    NULL,
    100.00
),

-- 2026 New Arrivals 100%
-- 신상품 컬렉션 완성 시 AW26 한정 카드 홀더 지급
(
    '90000000-0000-0000-0000-000000000003',
    '40000000-0000-0000-0000-000000000002',
    '70000000-0000-0000-0000-000000000002',
    NULL,
    100.00
),

-- Women's Signature 60%
-- 상품 5개 중 3개 보유 시 청담 특별전 해금
(
    '90000000-0000-0000-0000-000000000004',
    '40000000-0000-0000-0000-000000000003',
    NULL,
    '80000000-0000-0000-0000-000000000003',
    60.00
),

-- Global Travel Collection 100%
-- 트래블 컬렉션 완성 시 밀라노 전시 해금
(
    '90000000-0000-0000-0000-000000000005',
    '40000000-0000-0000-0000-000000000004',
    NULL,
    '80000000-0000-0000-0000-000000000002',
    100.00
),

-- MCM Icons 100%
-- 아이코닉 컬렉션 완성 시 프리미엄 케어 혜택 지급
(
    '90000000-0000-0000-0000-000000000006',
    '40000000-0000-0000-0000-000000000005',
    '70000000-0000-0000-0000-000000000003',
    NULL,
    100.00
);

-- ============================================================
-- 11. PURCHASE QRS
-- ============================================================

-- ============================================================
-- PURCHASE QRS
--
-- 매장 ID
-- 001: MCM 신세계면세점 본점, Seoul
-- 002: Ikebukuro Tobu, Tokyo
-- 003: MCM New York Soho, New York
-- ============================================================

INSERT INTO purchase_qrs (
    id,
    qr_token,
    product_id,
    store_id,
    purchase_date,
    serial_number,
    is_used,
    used_by,
    used_at,
    expires_at,
    created_at
) VALUES

-- 1. 실크 파자마 셔츠
(
    'a0000000-0000-0000-0000-000000000001',
    'MCM-DEMO-2026-001',
    '50000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    '2026-08-01 14:30:00',
    'MCM-2026-SEOUL-001',
    FALSE,
    NULL,
    NULL,
    '2027-08-01 23:59:59',
    CURRENT_TIMESTAMP
),

-- 2. 디스코 모노그램 프린트 쁘띠 스카프
(
    'a0000000-0000-0000-0000-000000000002',
    'MCM-DEMO-2026-002',
    '50000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    '2026-08-02 16:00:00',
    'MCM-2026-TOKYO-002',
    FALSE,
    NULL,
    NULL,
    '2027-08-02 23:59:59',
    CURRENT_TIMESTAMP
),

-- 3. Rockstar 골드 크리스탈 비세토스 베니티 케이스
(
    'a0000000-0000-0000-0000-000000000003',
    'MCM-DEMO-2026-003',
    '50000000-0000-0000-0000-000000000003',
    '30000000-0000-0000-0000-000000000001',
    '2026-08-03 13:20:00',
    'MCM-2026-SEOUL-003',
    FALSE,
    NULL,
    NULL,
    '2027-08-03 23:59:59',
    CURRENT_TIMESTAMP
),

-- 4. 코스믹 스타 오 드 퍼퓸
(
    'a0000000-0000-0000-0000-000000000004',
    'MCM-DEMO-2026-004',
    '50000000-0000-0000-0000-000000000004',
    '30000000-0000-0000-0000-000000000001',
    '2026-08-04 18:10:00',
    'MCM-2026-SEOUL-004',
    FALSE,
    NULL,
    NULL,
    '2027-08-04 23:59:59',
    CURRENT_TIMESTAMP
),

-- 5. Aren 다이아몬드 퀼팅 레더 백팩
(
    'a0000000-0000-0000-0000-000000000005',
    'MCM-DEMO-2026-005',
    '50000000-0000-0000-0000-000000000005',
    '30000000-0000-0000-0000-000000000003',
    '2026-08-05 15:40:00',
    'MCM-2026-NEWYORK-005',
    FALSE,
    NULL,
    NULL,
    '2027-08-05 23:59:59',
    CURRENT_TIMESTAMP
),

-- 6. 비세토스 수트케이스
(
    'a0000000-0000-0000-0000-000000000006',
    'MCM-DEMO-2026-006',
    '50000000-0000-0000-0000-000000000006',
    '30000000-0000-0000-0000-000000000003',
    '2026-08-06 12:00:00',
    'MCM-2026-NEWYORK-006',
    FALSE,
    NULL,
    NULL,
    '2027-08-06 23:59:59',
    CURRENT_TIMESTAMP
),

-- 7. Ottomar 비세토스 여권 케이스
(
    'a0000000-0000-0000-0000-000000000007',
    'MCM-DEMO-2026-007',
    '50000000-0000-0000-0000-000000000007',
    '30000000-0000-0000-0000-000000000002',
    '2026-08-07 17:25:00',
    'MCM-2026-TOKYO-007',
    FALSE,
    NULL,
    NULL,
    '2027-08-07 23:59:59',
    CURRENT_TIMESTAMP
),

-- 8. MCM Park 비세토스 토끼 인형
(
    'a0000000-0000-0000-0000-000000000008',
    'MCM-DEMO-2026-008',
    '50000000-0000-0000-0000-000000000008',
    '30000000-0000-0000-0000-000000000001',
    '2026-08-08 11:50:00',
    'MCM-2026-SEOUL-008',
    FALSE,
    NULL,
    NULL,
    '2027-08-08 23:59:59',
    CURRENT_TIMESTAMP
),

-- 9. 모노그램 프린트 양가죽 크로스 샌들
(
    'a0000000-0000-0000-0000-000000000009',
    'MCM-DEMO-2026-009',
    '50000000-0000-0000-0000-000000000009',
    '30000000-0000-0000-0000-000000000002',
    '2026-08-09 16:35:00',
    'MCM-2026-TOKYO-009',
    FALSE,
    NULL,
    NULL,
    '2027-08-09 23:59:59',
    CURRENT_TIMESTAMP
),

-- 10. New Liz 엠보스드 모노그램 레더 쇼퍼
(
    'a0000000-0000-0000-0000-000000000010',
    'MCM-DEMO-2026-010',
    '50000000-0000-0000-0000-000000000010',
    '30000000-0000-0000-0000-000000000003',
    '2026-08-10 14:15:00',
    'MCM-2026-NEWYORK-010',
    FALSE,
    NULL,
    NULL,
    '2027-08-10 23:59:59',
    CURRENT_TIMESTAMP
),

-- 11. Ella 비세토스 보스턴 백
(
    'a0000000-0000-0000-0000-000000000011',
    'MCM-DEMO-2026-011',
    '50000000-0000-0000-0000-000000000011',
    '30000000-0000-0000-0000-000000000001',
    '2026-08-11 19:00:00',
    'MCM-2026-SEOUL-011',
    FALSE,
    NULL,
    NULL,
    '2027-08-11 23:59:59',
    CURRENT_TIMESTAMP
);