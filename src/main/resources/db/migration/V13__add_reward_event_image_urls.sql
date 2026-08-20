-- ============================================================
-- V13: 리워드 및 이벤트 상세 이미지 URL 추가
-- 이미지 기준 경로:
-- src/main/resources/static/images/rewards
-- ============================================================

UPDATE rewards
SET image_url = '/images/rewards/reward_001_seoul_collector_pass.png'
WHERE id = '70000000-0000-0000-0000-000000000001'::UUID;

UPDATE rewards
SET image_url = '/images/rewards/reward_002_aw26_limited_card_holder.png'
WHERE id = '70000000-0000-0000-0000-000000000002'::UUID;

UPDATE rewards
SET image_url = '/images/rewards/reward_003_mcm_icons_premium_care.png'
WHERE id = '70000000-0000-0000-0000-000000000003'::UUID;

UPDATE events
SET image_url = '/images/rewards/event_001_lotte_special_promotion.png'
WHERE id = '80000000-0000-0000-0000-000000000001'::UUID;

UPDATE events
SET image_url = '/images/rewards/event_002_disco_on_mars.png'
WHERE id = '80000000-0000-0000-0000-000000000002'::UUID;

UPDATE events
SET image_url = '/images/rewards/event_003_from_munich_to_mars.png'
WHERE id = '80000000-0000-0000-0000-000000000003'::UUID;
