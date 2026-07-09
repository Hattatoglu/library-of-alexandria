CREATE TYPE book_type AS ENUM (
    'NOVEL',
    'HISTORY',
    'LITERATURE',
    'SCIENTIFIC',
    'SCIENCE_FICTION',
    'POEM',
    'CULTURE'
);

CREATE TYPE book_status AS ENUM (
    'AVAILABLE',
    'MAINTENANCE',
    'LOST',
    'REMOVED'
);

CREATE TABLE books (
    id              BIGSERIAL       PRIMARY KEY,
    book_id         UUID            NOT NULL UNIQUE,
    isbn            VARCHAR(20)     NOT NULL,
    book_name       VARCHAR(255)    NOT NULL,
    author          VARCHAR(255)    NOT NULL,
    publish_year    INTEGER        NOT NULL,
    type            book_type       NOT NULL,
    is_bc           BOOLEAN         NOT NULL,
    status          book_status     NOT NULL,
    available       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox (
    id              UUID            PRIMARY KEY,
    message         JSONB           NOT NULL,
    outbox_status   VARCHAR(50)     NOT NULL,
    service         VARCHAR(100)    NOT NULL,
    topic           VARCHAR(255)    NOT NULL
);

CREATE INDEX idx_books_book_id ON books (book_id);
CREATE INDEX idx_books_book_name ON books (book_name);
CREATE INDEX idx_books_author ON books (author);

CREATE INDEX idx_outbox_id ON outbox (id);
CREATE INDEX idx_outbox_outbox_status ON outbox (outbox_status);