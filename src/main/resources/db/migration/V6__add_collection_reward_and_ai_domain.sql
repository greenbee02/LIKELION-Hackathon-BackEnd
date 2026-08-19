-- ============================================================
-- V6: 유저 콜렉션 + AI 관련 DB
-- ============================================================


-- ============================================================
-- 1. USER COLLECTIONS
-- 사용자가 직접 만들거나 AI가 생성한 개인 컬렉션
-- ============================================================
CREATE TABLE collections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    cover_image_url VARCHAR(1000),
    collection_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    generation_reason TEXT,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_collections_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_collections_type
        CHECK (collection_type IN ('CUSTOM', 'AI'))
);

CREATE INDEX idx_collections_user_id
    ON collections (user_id);

CREATE INDEX idx_collections_user_type
    ON collections (user_id, collection_type);


-- ============================================================
-- 2. COLLECTION CARDS
-- 개인 컬렉션과 보유 카드의 다대다 연결
-- ============================================================
CREATE TABLE collection_cards (
    collection_id UUID NOT NULL,
    card_id UUID NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_collection_cards
        PRIMARY KEY (collection_id, card_id),

    CONSTRAINT fk_collection_cards_collection
        FOREIGN KEY (collection_id)
        REFERENCES collections (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_cards_card
        FOREIGN KEY (card_id)
        REFERENCES cards (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_collection_cards_card_id
    ON collection_cards (card_id);


-- ============================================================
-- 3. REWARDS
-- 컬렉션 달성으로 제공되는 리워드
-- ============================================================
CREATE TABLE rewards (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    reward_type VARCHAR(50) NOT NULL,
    image_url VARCHAR(1000),
    quantity INTEGER,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rewards_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_rewards_type
        CHECK (
            reward_type IN (
                'PHYSICAL_CARD',
                'GOODS',
                'EVENT_INVITATION',
                'BENEFIT'
            )
        ),

    CONSTRAINT chk_rewards_quantity
        CHECK (quantity IS NULL OR quantity >= 0)
);

CREATE INDEX idx_rewards_brand_id
    ON rewards (brand_id);

CREATE INDEX idx_rewards_active
    ON rewards (is_active);


-- ============================================================
-- 4. EVENTS
-- 프로모션, 전시, 초청 이벤트
-- ============================================================
CREATE TABLE events (
    id UUID PRIMARY KEY,
    brand_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(500),
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    capacity INTEGER,
    image_url VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_brand
        FOREIGN KEY (brand_id)
        REFERENCES brands (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_events_period
        CHECK (end_at >= start_at),

    CONSTRAINT chk_events_capacity
        CHECK (capacity IS NULL OR capacity >= 0)
);

CREATE INDEX idx_events_brand_id
    ON events (brand_id);

CREATE INDEX idx_events_start_at
    ON events (start_at);


-- ============================================================
-- 5. COLLECTION REWARD CONDITIONS
-- 공식 상품 컬렉션의 리워드 또는 이벤트 해금 조건
-- ============================================================
CREATE TABLE collection_rewards (
    id UUID PRIMARY KEY,
    product_collection_id UUID NOT NULL,
    reward_id UUID,
    event_id UUID,
    required_percentage NUMERIC(5, 2) NOT NULL DEFAULT 100.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_collection_rewards_product_collection
        FOREIGN KEY (product_collection_id)
        REFERENCES product_collections (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_rewards_reward
        FOREIGN KEY (reward_id)
        REFERENCES rewards (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_collection_rewards_event
        FOREIGN KEY (event_id)
        REFERENCES events (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_collection_rewards_target
        CHECK (
            (reward_id IS NOT NULL AND event_id IS NULL)
            OR
            (reward_id IS NULL AND event_id IS NOT NULL)
        ),

    CONSTRAINT chk_collection_rewards_percentage
        CHECK (
            required_percentage > 0
            AND required_percentage <= 100
        )
);

CREATE INDEX idx_collection_rewards_collection
    ON collection_rewards (product_collection_id);

CREATE INDEX idx_collection_rewards_reward
    ON collection_rewards (reward_id);

CREATE INDEX idx_collection_rewards_event
    ON collection_rewards (event_id);


-- ============================================================
-- 6. USER REWARDS
-- 사용자가 해금하거나 수령한 리워드 및 이벤트
-- ============================================================
CREATE TABLE user_rewards (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    reward_id UUID,
    event_id UUID,
    unlocked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'UNLOCKED',
    claim_code VARCHAR(100) UNIQUE,
    claimed_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_user_rewards_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_rewards_reward
        FOREIGN KEY (reward_id)
        REFERENCES rewards (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_user_rewards_event
        FOREIGN KEY (event_id)
        REFERENCES events (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_user_rewards_target
        CHECK (
            (reward_id IS NOT NULL AND event_id IS NULL)
            OR
            (reward_id IS NULL AND event_id IS NOT NULL)
        ),

    CONSTRAINT chk_user_rewards_status
        CHECK (
            status IN (
                'UNLOCKED',
                'CLAIMED',
                'EXPIRED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_user_rewards_claimed_state
        CHECK (
            (status = 'CLAIMED' AND claimed_at IS NOT NULL)
            OR
            (status <> 'CLAIMED')
        )
);

CREATE INDEX idx_user_rewards_user_id
    ON user_rewards (user_id);

CREATE INDEX idx_user_rewards_status
    ON user_rewards (status);

CREATE INDEX idx_user_rewards_reward
    ON user_rewards (reward_id);

CREATE INDEX idx_user_rewards_event
    ON user_rewards (event_id);


-- ============================================================
-- 7. AI COLLECTION ANALYSES
-- 사용자의 보유 카드와 컬렉션을 분석한 결과
-- ============================================================
CREATE TABLE ai_collection_analyses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_collection_id UUID,
    generated_collection_id UUID,
    analysis_type VARCHAR(50) NOT NULL,
    input_snapshot JSONB,
    result JSONB NOT NULL,
    summary TEXT,
    ai_model VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_collection_analyses_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_collection_analyses_source_collection
        FOREIGN KEY (source_collection_id)
        REFERENCES collections (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_collection_analyses_generated_collection
        FOREIGN KEY (generated_collection_id)
        REFERENCES collections (id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_ai_collection_analyses_user_id
    ON ai_collection_analyses (user_id);

CREATE INDEX idx_ai_collection_analyses_source_collection
    ON ai_collection_analyses (source_collection_id);

CREATE INDEX idx_ai_collection_analyses_generated_collection
    ON ai_collection_analyses (generated_collection_id);

CREATE INDEX idx_ai_collection_analyses_type
    ON ai_collection_analyses (analysis_type);


-- ============================================================
-- 8. AI COLLECTION RECOMMENDATIONS
-- AI 분석 결과에 포함되는 추천 상품
-- ============================================================
CREATE TABLE ai_collection_recommendations (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL,
    product_id UUID NOT NULL,
    reason TEXT,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_collection_recommendations_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES ai_collection_analyses (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_collection_recommendations_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE RESTRICT,

    CONSTRAINT uk_ai_collection_recommendations_item
        UNIQUE (analysis_id, product_id),

    CONSTRAINT chk_ai_collection_recommendations_priority
        CHECK (priority >= 0)
);

CREATE INDEX idx_ai_collection_recommendations_product
    ON ai_collection_recommendations (product_id);

CREATE INDEX idx_ai_collection_recommendations_priority
    ON ai_collection_recommendations (analysis_id, priority);


-- ============================================================
-- 9. AI EXPERIENCE RECOMMENDATIONS
-- 부족한 상품, 매장 또는 이벤트 경험 추천
-- ============================================================
CREATE TABLE ai_experience_recommendations (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    analysis_id UUID,
    product_id UUID,
    store_id UUID,
    event_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    reason TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'RECOMMENDED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_experience_recommendations_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_ai_experience_recommendations_analysis
        FOREIGN KEY (analysis_id)
        REFERENCES ai_collection_analyses (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_product
        FOREIGN KEY (product_id)
        REFERENCES products (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_store
        FOREIGN KEY (store_id)
        REFERENCES stores (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_ai_experience_recommendations_event
        FOREIGN KEY (event_id)
        REFERENCES events (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_ai_experience_recommendations_target
        CHECK (
            product_id IS NOT NULL
            OR store_id IS NOT NULL
            OR event_id IS NOT NULL
        ),

    CONSTRAINT chk_ai_experience_recommendation_status
        CHECK (
            status IN (
                'RECOMMENDED',
                'VIEWED',
                'ACCEPTED',
                'DISMISSED'
            )
        )
);

CREATE INDEX idx_ai_experience_recommendations_user
    ON ai_experience_recommendations (user_id);

CREATE INDEX idx_ai_experience_recommendations_analysis
    ON ai_experience_recommendations (analysis_id);

CREATE INDEX idx_ai_experience_recommendations_status
    ON ai_experience_recommendations (status);

CREATE INDEX idx_ai_experience_recommendations_product
    ON ai_experience_recommendations (product_id);

CREATE INDEX idx_ai_experience_recommendations_store
    ON ai_experience_recommendations (store_id);

CREATE INDEX idx_ai_experience_recommendations_event
    ON ai_experience_recommendations (event_id);