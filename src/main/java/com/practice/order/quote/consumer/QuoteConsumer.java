package com.practice.order.quote.consumer;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class QuoteConsumer {

    private final QuoteCache quoteCache;
    private final ApplicationEventPublisher publisher;
    private final boolean broadcastEnabled;

    public QuoteConsumer(QuoteCache quoteCache,
                         ApplicationEventPublisher publisher,
                         @Value("${quote.broadcast.enabled:true}") boolean broadcastEnabled) {
        this.quoteCache = quoteCache;
        this.publisher = publisher;
        this.broadcastEnabled = broadcastEnabled;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.group-id}")
    public void consume(QuoteEvent event) {
        LatestQuote latest = quoteCache.update(event);
        if (broadcastEnabled) {
            publisher.publishEvent(latest);
        }
    }
}
