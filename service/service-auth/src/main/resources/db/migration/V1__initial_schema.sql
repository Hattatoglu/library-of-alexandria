CREATE TABLE users
(
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    name                    VARCHAR(100) NOT NULL,
    username                VARCHAR(50)  NOT NULL UNIQUE,
    password                VARCHAR(255) NOT NULL,
    email                   VARCHAR(150) NOT NULL UNIQUE,
    birthdate               DATE,
    account_non_expired     BOOLEAN      NOT NULL        DEFAULT TRUE,
    account_non_locked      BOOLEAN      NOT NULL        DEFAULT TRUE,
    credentials_non_expired BOOLEAN      NOT NULL        DEFAULT TRUE,
    is_enabled              BOOLEAN      NOT NULL        DEFAULT TRUE,
    created_at              TIMESTAMP    NOT NULL        DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL        DEFAULT NOW()
);

CREATE TABLE user_roles
(
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_user_id ON users (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
