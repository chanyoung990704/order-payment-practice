package com.practice.order.quote.snapshot;

import com.practice.order.quote.domain.LatestQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QuoteSnapshotWriterIntegrationTest {

    @Autowired
    private QuoteSnapshotBatchWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM quote_snapshots");
    }

    @Test
    void 최초_시퀀스는_삽입된다() {
        writer.upsertBatch(List.of(quote("005930", 70_000, 1)));

        assertThat(count()).isEqualTo(1);
        assertThat(sequenceOf("005930")).isEqualTo(1);
    }

    @Test
    void 더_큰_시퀀스만_갱신하고_오래된_시퀀시는_무시한다() {
        writer.upsertBatch(List.of(quote("005930", 70_000, 10)));
        writer.upsertBatch(List.of(quote("005930", 69_000, 7)));
        writer.upsertBatch(List.of(quote("005930", 71_000, 12)));

        assertThat(sequenceOf("005930")).isEqualTo(12);
        assertThat(priceOf("005930")).isEqualTo(71_000);
    }

    @Test
    void 같은_시퀀스의_재처리는_가격을_덮어쓰지_않는다() {
        writer.upsertBatch(List.of(quote("005930", 70_000, 10)));
        writer.upsertBatch(List.of(quote("005930", 99_000, 10)));

        assertThat(priceOf("005930")).isEqualTo(70_000);
    }

    private LatestQuote quote(String symbol, long price, long seq) {
        return new LatestQuote(symbol, price, 100, seq, System.currentTimeMillis());
    }

    private int count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quote_snapshots", Integer.class);
    }

    private long sequenceOf(String symbol) {
        return jdbcTemplate.queryForObject(
                "SELECT last_sequence FROM quote_snapshots WHERE symbol = ?", Long.class, symbol);
    }

    private long priceOf(String symbol) {
        return jdbcTemplate.queryForObject(
                "SELECT price FROM quote_snapshots WHERE symbol = ?", Long.class, symbol);
    }
}
