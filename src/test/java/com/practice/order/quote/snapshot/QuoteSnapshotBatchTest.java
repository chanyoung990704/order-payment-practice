package com.practice.order.quote.snapshot;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QuoteSnapshotBatchTest {

    @Test
    void DB_저장_실패시_Offset을_커밋하지_않는다() {
        QuoteSnapshotBatchWriter writer = mock(QuoteSnapshotBatchWriter.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        QuoteSnapshotBatchConsumer consumer = new QuoteSnapshotBatchConsumer(writer);

        doThrow(new RuntimeException("DB 장애")).when(writer).upsertBatch(anyList());

        ConsumerRecord<String, QuoteEvent> record = new ConsumerRecord<>(
                "quote-events", 0, 10L, "005930",
                QuoteEvent.of("005930", 70_000, 100, 10));

        assertThatThrownBy(() -> consumer.consume(List.of(record), ack))
                .isInstanceOf(RuntimeException.class);

        verify(ack, never()).acknowledge();
    }

    @Test
    void DB_성공시_Offset을_커밋한다() {
        QuoteSnapshotBatchWriter writer = mock(QuoteSnapshotBatchWriter.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        QuoteSnapshotBatchConsumer consumer = new QuoteSnapshotBatchConsumer(writer);

        ConsumerRecord<String, QuoteEvent> record = new ConsumerRecord<>(
                "quote-events", 0, 10L, "005930",
                QuoteEvent.of("005930", 70_000, 100, 10));

        consumer.consume(List.of(record), ack);

        verify(writer).upsertBatch(anyList());
        verify(ack).acknowledge();
    }

    @Test
    void 같은_배치에서_종목별_최신_시퀀스만_남긴다() {
        QuoteSnapshotBatchWriter writer = mock(QuoteSnapshotBatchWriter.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        QuoteSnapshotBatchConsumer consumer = new QuoteSnapshotBatchConsumer(writer);

        List<ConsumerRecord<String, QuoteEvent>> records = List.of(
                record("005930", 70_000, 1),
                record("005930", 71_000, 3),
                record("005930", 70_500, 2),
                record("000660", 120_000, 1)
        );

        consumer.consume(records, ack);

        org.mockito.ArgumentCaptor<List<LatestQuote>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(writer).upsertBatch(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).filteredOn(q -> q.symbol().equals("005930"))
                .singleElement().satisfies(q -> {
                    assertThat(q.sequence()).isEqualTo(3);
                    assertThat(q.price()).isEqualTo(71_000);
                });
    }

    private ConsumerRecord<String, QuoteEvent> record(String symbol, long price, long seq) {
        return new ConsumerRecord<>("quote-events", 0, seq, symbol,
                QuoteEvent.of(symbol, price, 100, seq));
    }
}
