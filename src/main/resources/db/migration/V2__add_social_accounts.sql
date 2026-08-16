ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE social_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_social_accounts_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_social_accounts_user_id ON social_accounts (user_id);
