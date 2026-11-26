-- V2: índices de apoyo para las consultas de los repositorios.
-- Las unicidades de username/email/account_number ya vienen de V1.

CREATE INDEX idx_accounts_owner_id ON accounts (owner_id);

CREATE INDEX idx_transactions_from_account_id ON transactions (from_account_id);
CREATE INDEX idx_transactions_to_account_id ON transactions (to_account_id);
CREATE INDEX idx_transactions_timestamp ON transactions (timestamp);
