package com.practice.order.quote.archive;

import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuoteArchiveConsumer {

    private final Map<String, QuoteEvent> archived = new ConcurrentHashMap<>();

    @KafkaListener(topics = "${kafka.topic}", groupId = "${quote.archive.group-id}")
    public void archive(QuoteEvent event) {
        // 실구현: (symbol, sequence)를 PK로 append-only 저장 → 중복 적재 멱등
        archived.put(event.symbol() + ":" + event.sequence(), event);
    }

    public int archivedCount() {
        return archived.size();
    }
}
