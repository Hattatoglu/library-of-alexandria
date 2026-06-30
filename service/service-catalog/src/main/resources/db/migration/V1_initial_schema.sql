CREATE TYPE book_type AS ENUM (
    'NOVEL',
    'HISTORY',
    'LITERATURE',
    'SCIENTIFIC',
    'SCIENCE_FICTION',
    'POEM',
    'CULTURE'
);

CREATE TABLE book (
    id              BIGSERIAL       PRIMARY KEY,
    book_id         UUID            NOT NULL UNIQUE,
    isbn            VARCHAR(20)     NOT NULL,
    book_name       VARCHAR(255)    NOT NULL,
    author          VARCHAR(255)    NOT NULL,
    publish_year    SMALLINT        NOT NULL,
    type            book_type       NOT NULL,
    is_bc           BOOLEAN         NOT NULL,
    available       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_books_book_id ON books (book_id);
CREATE INDEX idx_books_name ON books (name);
CREATE INDEX idx_books_author ON books (author);