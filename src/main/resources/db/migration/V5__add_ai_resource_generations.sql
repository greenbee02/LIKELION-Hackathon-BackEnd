-- AI가 생성한 카드 리소스 후보를 카드 커스터마이징 이력과 분리해 관리한다.
-- 실제 이미지 생성 provider가 완료하면 PENDING 행의 결과 컬럼과 상태를 갱신한다.

CREATE TABLE ai_resource_generations (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL,
    product_id UUID,
    template_id UUID,
    resource_type VARCHAR(30) NOT NULL,
    prompt VARCHAR(2000),
    source_image_url VARCHAR(1000),
    generated_image_url VARCHAR(1000),
    generated_data TEXT,
    ai_model VARCHAR(100),
    generation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    failure_reason VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_resource_generations_card
        FOREIGN KEY (card_id) REFERENCES cards (id),
    CONSTRAINT fk_ai_resource_generations_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_ai_resource_generations_template
        FOREIGN KEY (template_id) REFERENCES card_templates (id),
    CONSTRAINT chk_ai_resource_generations_type
        CHECK (resource_type IN (
            'BACKGROUND', 'BORDER', 'PATTERN', 'PRODUCT_ANGLE',
            'DECORATION', 'COLOR_PALETTE', 'TEXT_STYLE', 'COMPOSITION'
        )),
    CONSTRAINT chk_ai_resource_generations_status
        CHECK (generation_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REJECTED', 'ARCHIVED'))
);

CREATE INDEX idx_ai_resource_generations_card_id
    ON ai_resource_generations (card_id);
CREATE INDEX idx_ai_resource_generations_status
    ON ai_resource_generations (generation_status);
CREATE INDEX idx_ai_resource_generations_type
    ON ai_resource_generations (resource_type);
