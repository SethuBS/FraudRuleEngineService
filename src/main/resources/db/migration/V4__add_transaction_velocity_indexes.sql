CREATE INDEX idx_transactions_customer_transaction_timestamp
    ON transactions (customer_id, transaction_timestamp);

CREATE INDEX idx_transactions_account_transaction_timestamp
    ON transactions (account_id, transaction_timestamp);
