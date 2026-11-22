CREATE TABLE IF NOT EXISTS quote_snapshots (
    symbol VARCHAR(32) PRIMARY KEY,
    price BIGINT NOT NULL,
    volume BIGINT NOT NULL,
    last_sequence BIGINT NOT NULL,
    updated_at_epoch_milli BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
