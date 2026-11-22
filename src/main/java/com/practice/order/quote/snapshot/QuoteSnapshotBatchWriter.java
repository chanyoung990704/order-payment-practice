package com.practice.order.quote.snapshot;

import com.practice.order.quote.domain.LatestQuote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuoteSnapshotBatchWriter {

    private static final String UPDATE_IF_NEWER_SQL = """
            UPDATE quote_snapshots SET
                price = CASE WHEN ? > last_sequence THEN ? ELSE price END,
                volume = CASE WHEN ? > last_sequence THEN ? ELSE volume END,
                updated_at_epoch_milli = CASE WHEN ? > last_sequence THEN ? ELSE updated_at_epoch_milli END,
                version = CASE WHEN ? > last_sequence THEN version + 1 ELSE version END,
                last_sequence = CASE WHEN ? > last_sequence THEN ? ELSE last_sequence END
            WHERE symbol = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO quote_snapshots
                (symbol, price, volume, last_sequence, updated_at_epoch_milli, version)
            VALUES (?, ?, ?, ?, ?, 0)
            """;

    private final JdbcTemplate jdbcTemplate;

    public QuoteSnapshotBatchWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void upsertBatch(List<LatestQuote> quotes) {
        if (quotes.isEmpty()) {
            return;
        }
        for (LatestQuote quote : quotes) {
            int updated = jdbcTemplate.update(UPDATE_IF_NEWER_SQL,
                    quote.sequence(), quote.price(),
                    quote.sequence(), quote.volume(),
                    quote.sequence(), quote.updatedAtEpochMilli(),
                    quote.sequence(), quote.sequence(), quote.sequence(),
                    quote.symbol());

            if (updated == 0) {
                jdbcTemplate.update(INSERT_SQL,
                        quote.symbol(), quote.price(), quote.volume(),
                        quote.sequence(), quote.updatedAtEpochMilli());
            }
        }
    }
}
