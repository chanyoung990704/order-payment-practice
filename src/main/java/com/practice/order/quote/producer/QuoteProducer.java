package com.practice.order.quote.producer;

import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class QuoteProducer {

    private final KafkaTemplate<String, QuoteEvent> kafkaTemplate;
    private final String topic;

    public QuoteProducer(KafkaTemplate<String, QuoteEvent> kafkaTemplate,
                         @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(QuoteEvent event) {
        // 종목별 파티션 순서 보장: 같은 symbol은 항상 같은 파티션으로
        kafkaTemplate.send(topic, event.symbol(), event);
    }

    public static byte[] partitionKey(String symbol) {
        return symbol.getBytes(StandardCharsets.UTF_8);
    }
}
