-- 하나의 리소스 종류에 대한 3~4개 후보를 묶어 관리한다.
-- 기존 단일 생성 이력은 NULL 값으로 그대로 호환한다.

ALTER TABLE ai_resource_generations
    ADD COLUMN candidate_group_id UUID,
    ADD COLUMN candidate_index INTEGER,
    ADD COLUMN candidate_count INTEGER;

CREATE INDEX idx_ai_resource_generations_candidate_group
    ON ai_resource_generations (candidate_group_id);

ALTER TABLE ai_resource_generations
    ADD CONSTRAINT chk_ai_resource_generations_candidate_range
    CHECK (
        candidate_group_id IS NULL
        OR (
            candidate_index IS NOT NULL
            AND candidate_count IN (3, 4)
            AND candidate_index BETWEEN 1 AND candidate_count
        )
    );
