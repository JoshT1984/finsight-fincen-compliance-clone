-- ======================================
-- Transactions + CTR Service Schema
-- ======================================

CREATE TABLE IF NOT EXISTS cash_transaction (
  transaction_id BIGSERIAL PRIMARY KEY,
  customer_name VARCHAR(128) NOT NULL,
  amount NUMERIC(14,2) NOT NULL CHECK (amount >= 0),
  direction VARCHAR(16) NOT NULL CHECK (direction IN ('DEPOSIT','WITHDRAWAL')),
  transaction_time TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ctr (
  ctr_id BIGSERIAL PRIMARY KEY,
  customer_name VARCHAR(128) NOT NULL, -- snapshot / authoritative name
  transaction_date DATE NOT NULL,
  total_amount NUMERIC(14,2) NOT NULL CHECK (total_amount >= 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (customer_name, transaction_date)
);

CREATE INDEX IF NOT EXISTS idx_cash_tx_time ON cash_transaction(transaction_time);
CREATE INDEX IF NOT EXISTS idx_ctr_created ON ctr(created_at);
