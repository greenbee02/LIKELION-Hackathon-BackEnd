-- ============================================================
-- 1. DATABASE SUMMARY
-- 전체 테이블별 데이터 개수와 상태를 한 번에 확인한다.
-- ============================================================

SELECT
    table_name,
    row_count,
    CASE
        WHEN row_count = 0 THEN 'EMPTY'
        ELSE 'OK'
    END AS data_status
FROM (
    SELECT 1 AS sort_order, 'users' AS table_name, COUNT(*) AS row_count FROM users
    UNION ALL SELECT 2, 'social_accounts', COUNT(*) FROM social_accounts
    UNION ALL SELECT 3, 'brands', COUNT(*) FROM brands
    UNION ALL SELECT 4, 'stores', COUNT(*) FROM stores
    UNION ALL SELECT 5, 'product_collections', COUNT(*) FROM product_collections
    UNION ALL SELECT 6, 'products', COUNT(*) FROM products
    UNION ALL SELECT 7, 'product_collection_items', COUNT(*) FROM product_collection_items
    UNION ALL SELECT 8, 'purchase_qrs', COUNT(*) FROM purchase_qrs
    UNION ALL SELECT 9, 'card_templates', COUNT(*) FROM card_templates
    UNION ALL SELECT 10, 'cards', COUNT(*) FROM cards
    UNION ALL SELECT 11, 'card_customizations', COUNT(*) FROM card_customizations
    UNION ALL SELECT 12, 'collections', COUNT(*) FROM collections
    UNION ALL SELECT 13, 'collection_cards', COUNT(*) FROM collection_cards
    UNION ALL SELECT 14, 'rewards', COUNT(*) FROM rewards
    UNION ALL SELECT 15, 'events', COUNT(*) FROM events
    UNION ALL SELECT 16, 'collection_rewards', COUNT(*) FROM collection_rewards
    UNION ALL SELECT 17, 'user_rewards', COUNT(*) FROM user_rewards
    UNION ALL SELECT 18, 'physical_cards', COUNT(*) FROM physical_cards
    UNION ALL SELECT 19, 'ai_collection_analyses', COUNT(*) FROM ai_collection_analyses
    UNION ALL SELECT 20, 'ai_collection_recommendations', COUNT(*) FROM ai_collection_recommendations
    UNION ALL SELECT 21, 'ai_experience_recommendations', COUNT(*) FROM ai_experience_recommendations
) AS database_summary
ORDER BY sort_order;


-- ============================================================
-- 2. SCHEMA TABLE LIST
-- 실제 생성된 테이블과 대략적인 행 수를 확인한다.
-- ============================================================

SELECT
    table_name,
    engine,
    table_rows,
    table_collation,
    create_time
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;


-- ============================================================
-- 3. BRAND AND STORES
-- ============================================================

SELECT
    b.name AS brand_name,
    s.name AS store_name,
    s.country,
    s.city,
    s.address,
    s.store_type
FROM brands b
LEFT JOIN stores s
    ON s.brand_id = b.id
ORDER BY b.name, s.country, s.city;


-- ============================================================
-- 4. PRODUCTS WITH COLLECTIONS
-- 상품별 기본 정보와 포함된 공식 컬렉션을 한 행에서 확인한다.
-- ============================================================

SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.offering_type,
    p.category,
    p.theme,
    p.production_year,
    p.season,
    p.region,
    p.color,
    p.price,
    p.image_url,
    GROUP_CONCAT(
        pc.name
        ORDER BY pci.display_order
        SEPARATOR ', '
    ) AS collections
FROM products p
LEFT JOIN product_collection_items pci
    ON pci.product_id = p.id
LEFT JOIN product_collections pc
    ON pc.id = pci.product_collection_id
GROUP BY
    p.id,
    p.name,
    p.offering_type,
    p.category,
    p.theme,
    p.production_year,
    p.season,
    p.region,
    p.color,
    p.price,
    p.image_url
ORDER BY p.name;


-- ============================================================
-- 5. COLLECTION DETAILS
-- 컬렉션별 필수 상품 수와 상품 목록을 확인한다.
-- ============================================================

SELECT
    pc.id AS collection_id,
    pc.name AS collection_name,
    pc.theme,
    pc.production_year,
    pc.season,
    pc.region,
    pc.is_limited,
    COUNT(pci.product_id) AS total_products,
    SUM(CASE WHEN pci.is_required = TRUE THEN 1 ELSE 0 END) AS required_products,
    GROUP_CONCAT(
        CONCAT(pci.display_order, '. ', p.name)
        ORDER BY pci.display_order
        SEPARATOR ' / '
    ) AS product_list
FROM product_collections pc
LEFT JOIN product_collection_items pci
    ON pci.product_collection_id = pc.id
LEFT JOIN products p
    ON p.id = pci.product_id
GROUP BY
    pc.id,
    pc.name,
    pc.theme,
    pc.production_year,
    pc.season,
    pc.region,
    pc.is_limited
ORDER BY pc.name;


-- ============================================================
-- 6. CARD TEMPLATES
-- 양면 템플릿 이미지와 활성 상태를 확인한다.
-- ============================================================

SELECT
    ct.id AS template_id,
    b.name AS brand_name,
    ct.name AS template_name,
    ct.front_image_url,
    ct.back_image_url,
    ct.is_active,
    ct.resource_data
FROM card_templates ct
JOIN brands b
    ON b.id = ct.brand_id
ORDER BY ct.name;


-- ============================================================
-- 7. REWARD AND EVENT CONDITIONS
-- 컬렉션별 달성 조건과 해금 대상을 확인한다.
-- ============================================================

SELECT
    pc.name AS collection_name,
    cr.required_percentage,
    CASE
        WHEN cr.reward_id IS NOT NULL THEN 'REWARD'
        WHEN cr.event_id IS NOT NULL THEN 'EVENT'
        ELSE 'INVALID'
    END AS unlock_type,
    COALESCE(r.name, e.name) AS unlock_name,
    r.reward_type,
    r.quantity AS reward_quantity,
    e.location AS event_location,
    e.start_at AS event_start_at,
    e.end_at AS event_end_at,
    COALESCE(r.is_active, e.is_active) AS is_active
FROM collection_rewards cr
JOIN product_collections pc
    ON pc.id = cr.product_collection_id
LEFT JOIN rewards r
    ON r.id = cr.reward_id
LEFT JOIN events e
    ON e.id = cr.event_id
ORDER BY pc.name, cr.required_percentage;


-- ============================================================
-- 8. CURRENT EVENTS
-- 현재 진행 중인 이벤트만 확인한다.
-- ============================================================

SELECT
    id,
    name,
    location,
    start_at,
    end_at,
    capacity
FROM events
WHERE is_active = TRUE
  AND NOW() BETWEEN start_at AND end_at
ORDER BY end_at;


-- ============================================================
-- 9. PURCHASE QR DASHBOARD
-- QR별 상품, 구매 매장, 사용 및 만료 상태를 확인한다.
-- ============================================================

SELECT
    pq.id AS purchase_qr_id,
    pq.qr_token,
    p.name AS product_name,
    s.name AS store_name,
    CONCAT(s.city, ', ', s.country) AS purchase_location,
    pq.purchase_date,
    pq.serial_number,
    CASE
        WHEN pq.is_used = TRUE THEN 'USED'
        WHEN pq.expires_at IS NOT NULL AND pq.expires_at <= NOW() THEN 'EXPIRED'
        ELSE 'AVAILABLE'
    END AS qr_status,
    pq.used_by,
    pq.used_at,
    pq.expires_at
FROM purchase_qrs pq
JOIN products p
    ON p.id = pq.product_id
JOIN stores s
    ON s.id = pq.store_id
ORDER BY pq.purchase_date, pq.qr_token;


-- ============================================================
-- 10. ISSUED CARD DASHBOARD
-- QR 사용 후 생성된 디지털 카드와 선택된 커스터마이징을 확인한다.
-- Seed 직후에는 결과가 없어도 정상이다.
-- ============================================================

SELECT
    c.id AS card_id,
    u.email AS owner_email,
    p.name AS product_name,
    c.card_type,
    c.status AS card_status,
    ct.name AS template_name,
    s.name AS purchase_store,
    c.purchase_date,
    c.serial_number,
    cc.id AS selected_customization_id,
    cc.generated_front_image_url,
    cc.generated_back_image_url
FROM cards c
JOIN users u
    ON u.id = c.user_id
JOIN products p
    ON p.id = c.product_id
JOIN card_templates ct
    ON ct.id = c.template_id
JOIN stores s
    ON s.id = c.purchase_store_id
LEFT JOIN card_customizations cc
    ON cc.id = c.selected_customization_id
ORDER BY c.created_at DESC;


-- ============================================================
-- 11. DATA INTEGRITY CHECK
-- 모든 issue_count가 0이면 참조 데이터가 정상이다.
-- ============================================================

SELECT
    check_name,
    issue_count,
    CASE
        WHEN issue_count = 0 THEN 'OK'
        ELSE 'CHECK_REQUIRED'
    END AS check_status
FROM (
    SELECT
        'Products without valid brand' AS check_name,
        COUNT(*) AS issue_count
    FROM products p
    LEFT JOIN brands b ON b.id = p.brand_id
    WHERE b.id IS NULL

    UNION ALL

    SELECT
        'Collection items without valid product or collection',
        COUNT(*)
    FROM product_collection_items pci
    LEFT JOIN products p ON p.id = pci.product_id
    LEFT JOIN product_collections pc ON pc.id = pci.product_collection_id
    WHERE p.id IS NULL OR pc.id IS NULL

    UNION ALL

    SELECT
        'Purchase QRs without valid product or store',
        COUNT(*)
    FROM purchase_qrs pq
    LEFT JOIN products p ON p.id = pq.product_id
    LEFT JOIN stores s ON s.id = pq.store_id
    WHERE p.id IS NULL OR s.id IS NULL

    UNION ALL

    SELECT
        'Purchase QRs with inconsistent used state',
        COUNT(*)
    FROM purchase_qrs pq
    WHERE
        (pq.is_used = FALSE AND (pq.used_by IS NOT NULL OR pq.used_at IS NOT NULL))
        OR
        (pq.is_used = TRUE AND (pq.used_by IS NULL OR pq.used_at IS NULL))

    UNION ALL

    SELECT
        'Reward conditions with invalid target count',
        COUNT(*)
    FROM collection_rewards cr
    WHERE
        (cr.reward_id IS NULL AND cr.event_id IS NULL)
        OR
        (cr.reward_id IS NOT NULL AND cr.event_id IS NOT NULL)

    UNION ALL

    SELECT
        'Cards without valid template',
        COUNT(*)
    FROM cards c
    LEFT JOIN card_templates ct ON ct.id = c.template_id
    WHERE ct.id IS NULL

    UNION ALL

    SELECT
        'Cards with invalid selected customization',
        COUNT(*)
    FROM cards c
    LEFT JOIN card_customizations cc
        ON cc.id = c.selected_customization_id
    WHERE c.selected_customization_id IS NOT NULL
      AND (cc.id IS NULL OR cc.card_id <> c.id)
) integrity_checks;


-- ============================================================
-- 12. OPTIONAL DELETE: RUNTIME DATA ONLY
-- 아래 블록은 기본적으로 실행되지 않는다.
-- 시연 중 생성된 카드, 개인 컬렉션, AI 결과만 초기화하려면
-- 각 줄의 '-- '를 제거한 뒤 블록 전체를 실행한다.
-- ============================================================

-- START TRANSACTION;
-- DELETE FROM ai_experience_recommendations;
-- DELETE FROM ai_collection_recommendations;
-- DELETE FROM ai_collection_analyses;
-- DELETE FROM physical_cards;
-- DELETE FROM collection_cards;
-- DELETE FROM collections;
-- DELETE FROM user_rewards;
-- DELETE FROM card_customizations;
-- DELETE FROM cards;
-- UPDATE purchase_qrs
-- SET
--     is_used = FALSE,
--     used_by = NULL,
--     used_at = NULL;
-- COMMIT;


-- ============================================================
-- 13. OPTIONAL DELETE: ALL DATA
-- 위험: 모든 Seed 및 서비스 데이터를 삭제한다.
-- 필요할 때만 아래 줄의 '-- '를 제거하고 블록 전체를 실행한다.
-- 테이블 구조는 유지된다.
-- ============================================================

-- SET FOREIGN_KEY_CHECKS = 0;
-- DELETE FROM ai_experience_recommendations;
-- DELETE FROM ai_collection_recommendations;
-- DELETE FROM ai_collection_analyses;
-- DELETE FROM physical_cards;
-- DELETE FROM collection_cards;
-- DELETE FROM collections;
-- DELETE FROM user_rewards;
-- DELETE FROM collection_rewards;
-- DELETE FROM events;
-- DELETE FROM rewards;
-- DELETE FROM card_customizations;
-- DELETE FROM cards;
-- DELETE FROM purchase_qrs;
-- DELETE FROM card_templates;
-- DELETE FROM product_collection_items;
-- DELETE FROM products;
-- DELETE FROM product_collections;
-- DELETE FROM stores;
-- DELETE FROM social_accounts;
-- DELETE FROM brands;
-- DELETE FROM users;
-- SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- 14. OPTIONAL DELETE: DATABASE
-- 가장 위험한 작업이다. DB 전체를 삭제할 때만 직접 실행한다.
-- ============================================================

-- DROP DATABASE luxury_collection;
