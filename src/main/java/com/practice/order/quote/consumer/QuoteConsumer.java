package com.practice.order.quote.consumer;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import com.practice.order.quote.store.QuoteStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class QuoteConsumer {

    private final QuoteStore quoteStore;
    private final ApplicationEventPublisher publisher;
    private final boolean broadcastEnabled;

    public QuoteConsumer(@Qualifier("localQuoteStore") QuoteStore quoteStore,
                         ApplicationEventPublisher publisher,
                         @Value("${quote.broadcast.enabled:true}") boolean broadcastEnabled) {
        this.quoteStore = quoteStore;
        this.publisher = publisher;
        this.broadcastEnabled = broadcastEnabled;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.group-id}")
    public void consume(QuoteEvent event) {
        LatestQuote latest = quoteStore.update(event);
        if (broadcastEnabled) {
            publisher.publishEvent(latest);
        }
    }
}
