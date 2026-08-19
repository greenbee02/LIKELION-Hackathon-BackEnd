ALTER TABLE ai_resource_generations
    DROP CONSTRAINT chk_ai_resource_generations_status;

ALTER TABLE ai_resource_generations
    ADD CONSTRAINT chk_ai_resource_generations_status
    CHECK (generation_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'REJECTED', 'ARCHIVED'));

ALTER TABLE ai_resource_generations
    ADD COLUMN processing_started_at TIMESTAMPTZ,
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMPTZ;

CREATE INDEX idx_ai_resource_generations_pending_schedule
    ON ai_resource_generations (generation_status, next_attempt_at, created_at);

CREATE INDEX idx_ai_resource_generations_processing_started
    ON ai_resource_generations (generation_status, processing_started_at);
