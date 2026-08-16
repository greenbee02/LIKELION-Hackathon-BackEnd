CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    season VARCHAR(100),
    region VARCHAR(100),
    material VARCHAR(100),
    color VARCHAR(100),
    warranty_info VARCHAR(1000),
    care_info VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cards (
    id UUID PRIMARY KEY,
    card_token VARCHAR(255) NOT NULL UNIQUE,
    product_id UUID NOT NULL,
    owner_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'UNREGISTERED',
    purchased_at TIMESTAMP WITH TIME ZONE,
    purchased_store VARCHAR(255),
    registered_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_cards_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_cards_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE TABLE card_customizations (
    id UUID PRIMARY KEY,
    card_id UUID NOT NULL UNIQUE,
    template_id UUID,
    image_url VARCHAR(1000),
    message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_card_customizations_card FOREIGN KEY (card_id) REFERENCES cards (id)
);

CREATE INDEX idx_cards_owner_id ON cards (owner_id);
CREATE INDEX idx_cards_product_id ON cards (product_id);
