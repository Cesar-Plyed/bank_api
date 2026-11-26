-- V3: integridad referencial (spec-11). Flyway es el único dueño del esquema
-- (datasources.default.schema-generate=NONE).
--
-- Antes de aplicar esta migración se eliminaron las filas huérfanas verificadas:
-- 88 accounts sin usuario y 28 transactions (4 con from_account_id inexistente,
-- 4 con to_account_id inexistente y 20 que referenciaban las cuentas eliminadas).
-- El resultado verificado fue 0 huérfanos en las tres relaciones.
--
-- Las FK de transactions usan ON DELETE SET NULL: el API permite borrar una
-- cuenta con saldo 0 (DELETE /api/accounts/{id} -> 204) aunque tenga historial,
-- y esas filas de Transaction se conservan (sin referencia a la cuenta ya ida).
-- La FK de accounts.owner_id no lleva ON DELETE: no se borran usuarios con cuentas.

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_owner_id
    FOREIGN KEY (owner_id) REFERENCES users (id);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_from_account_id
    FOREIGN KEY (from_account_id) REFERENCES accounts (id) ON DELETE SET NULL;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_to_account_id
    FOREIGN KEY (to_account_id) REFERENCES accounts (id) ON DELETE SET NULL;
