package com.practice.order.quote.snapshot;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuoteSnapshotBatchConsumer {

    private final QuoteSnapshotBatchWriter writer;

    public QuoteSnapshotBatchConsumer(QuoteSnapshotBatchWriter writer) {
        this.writer = writer;
    }

    @KafkaListener(
            topics = "${kafka.topic}",
            groupId = "${kafka.snapshot.group-id:quote-snapshot-group}",
            containerFactory = "quoteSnapshotBatchFactory"
    )
    public void consume(List<ConsumerRecord<String, QuoteEvent>> records,
                        Acknowledgment ack) {

        Map<String, LatestQuote> latest = new LinkedHashMap<>();
        for (ConsumerRecord<String, QuoteEvent> record : records) {
            LatestQuote incoming = LatestQuote.from(record.value());
            latest.merge(incoming.symbol(), incoming, (oldValue, newValue) ->
                    newValue.sequence() > oldValue.sequence() ? newValue : oldValue);
        }

        writer.upsertBatch(new ArrayList<>(latest.values()));
        ack.acknowledge();
    }
}
