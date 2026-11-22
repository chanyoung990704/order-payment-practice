package com.practice.order.quote;

import com.practice.order.quote.consumer.QuoteCache;
import com.practice.order.quote.consumer.QuoteConsumer;
import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class QuotePipelineTest {

    private QuoteCache quoteCache;
    private QuoteConsumer quoteConsumer;
    private SimpMessagingTemplate messagingTemplate;
    private ApplicationEventPublisher publisher;
    private final AtomicInteger published = new AtomicInteger();

    @BeforeEach
    void setUp() {
        quoteCache = new QuoteCache();
        messagingTemplate = mock(SimpMessagingTemplate.class);
        publisher = event -> published.incrementAndGet();
        quoteConsumer = new QuoteConsumer(quoteCache, publisher, true);
        quoteCache.clear();
        published.set(0);
    }

    @Test
    void 시세가_들어오면_최신가가_갱신된다() {
        quoteConsumer.consume(QuoteEvent.of("005930", 70_000, 100, 1));

        LatestQuote latest = quoteCache.get("005930");
        assertThat(latest.price()).isEqualTo(70_000);
        assertThat(latest.volume()).isEqualTo(100);
    }

    @Test
    void 순서가_뒤집혀_들어와도_시퀀스가_큰_데이터가_유지된다() {
        quoteConsumer.consume(QuoteEvent.of("005930", 71_000, 200, 3));
        quoteConsumer.consume(QuoteEvent.of("005930", 70_500, 150, 2));
        quoteConsumer.consume(QuoteEvent.of("005930", 70_000, 100, 1));

        LatestQuote latest = quoteCache.get("005930");
        assertThat(latest.sequence()).isEqualTo(3);
        assertThat(latest.price()).isEqualTo(71_000);
    }

    @Test
    void 종목별로_독립적으로_최신가가_관리된다() {
        quoteConsumer.consume(QuoteEvent.of("005930", 70_000, 100, 1));
        quoteConsumer.consume(QuoteEvent.of("000660", 120_000, 50, 1));

        assertThat(quoteCache.snapshot()).hasSize(2);
        assertThat(quoteCache.get("005930").price()).isEqualTo(70_000);
        assertThat(quoteCache.get("000660").price()).isEqualTo(120_000);
    }

    @Test
    void 각_시세_이벤트는_웹소켓_발행_이벤트를_만든다() {
        quoteConsumer.consume(QuoteEvent.of("005930", 70_000, 100, 1));
        quoteConsumer.consume(QuoteEvent.of("005930", 70_100, 120, 2));

        assertThat(published.get()).isEqualTo(2);
    }

    @Test
    void 발행을_끄면_캐시만_갱신된다() {
        QuoteConsumer noBroadcast = new QuoteConsumer(quoteCache, publisher, false);
        noBroadcast.consume(QuoteEvent.of("005930", 70_000, 100, 1));

        assertThat(quoteCache.get("005930").price()).isEqualTo(70_000);
        assertThat(published.get()).isZero();
    }
}
