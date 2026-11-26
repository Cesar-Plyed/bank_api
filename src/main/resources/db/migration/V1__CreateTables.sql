-- V1: esquema inicial. Flyway es el único dueño del esquema
-- (datasources.default.schema-generate=NONE).

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    username      VARCHAR(80)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE accounts (
    id             UUID PRIMARY KEY,
    owner_id       UUID          NOT NULL,
    account_number VARCHAR(50)   NOT NULL UNIQUE,
    currency       VARCHAR(3)    NOT NULL,
    balance        NUMERIC(19,2) NOT NULL DEFAULT 0,
    account_type   VARCHAR(32)   NOT NULL,
    status         VARCHAR(32)   NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL
);

CREATE TABLE transactions (
    id              UUID PRIMARY KEY,
    from_account_id UUID,
    to_account_id   UUID,
    amount          NUMERIC(19,2) NOT NULL,
    type            VARCHAR(32)   NOT NULL,
    status          VARCHAR(32)   NOT NULL,
    timestamp       TIMESTAMPTZ   NOT NULL,
    description     VARCHAR(500)
);
