package com.practice.order.quote.domain;

public record LatestQuote(
        String symbol,
        long price,
        long volume,
        long sequence,
        long updatedAtEpochMilli
) {
    public static LatestQuote from(QuoteEvent event) {
        return new LatestQuote(event.symbol(), event.price(), event.volume(),
                event.sequence(), event.eventTime().toEpochMilli());
    }
}
