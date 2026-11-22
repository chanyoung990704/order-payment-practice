package com.practice.order.quote;

import com.practice.order.quote.controller.QuoteBroadcastListener;
import com.practice.order.quote.domain.LatestQuote;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class QuoteBroadcastTest {

    @Test
    void 종목별_토픽으로_시세가_전송된다() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        QuoteBroadcastListener listener = new QuoteBroadcastListener(template);
        LatestQuote quote = new LatestQuote("005930", 70_000, 100, 1, 1234L);

        listener.onQuote(quote);

        verify(template).convertAndSend("/topic/quotes/005930", quote);
    }
}
