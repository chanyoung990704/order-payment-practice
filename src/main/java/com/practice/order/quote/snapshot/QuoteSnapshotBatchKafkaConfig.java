package com.practice.order.quote.snapshot;

import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class QuoteSnapshotBatchKafkaConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, QuoteEvent>
            quoteSnapshotBatchFactory(ConsumerFactory<String, QuoteEvent> consumerFactory) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, QuoteEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
                new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }
}
