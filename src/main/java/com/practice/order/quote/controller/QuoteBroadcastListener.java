package com.practice.order.quote.controller;

import com.practice.order.quote.domain.LatestQuote;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class QuoteBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    public QuoteBroadcastListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @EventListener
    public void onQuote(LatestQuote quote) {
        messagingTemplate.convertAndSend("/topic/quotes/" + quote.symbol(), quote);
    }
}
