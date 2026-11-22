package com.practice.order.quote.domain;

import java.time.Instant;

public record QuoteEvent(
        String symbol,
        long price,
        long volume,
        long sequence,
        Instant eventTime
) {
    public static QuoteEvent of(String symbol, long price, long volume, long sequence) {
        return new QuoteEvent(symbol, price, volume, sequence, Instant.now());
    }
}
