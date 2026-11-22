package com.practice.order.quote.store;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("localQuoteStore")
public class LocalQuoteCache implements QuoteStore {

    private final Map<String, LatestQuote> latestBySymbol = new ConcurrentHashMap<>();

    @Override
    public LatestQuote update(QuoteEvent event) {
        LatestQuote candidate = LatestQuote.from(event);
        latestBySymbol.merge(event.symbol(), candidate, (current, incoming) ->
                incoming.sequence() >= current.sequence() ? incoming : current);
        return latestBySymbol.get(event.symbol());
    }

    @Override
    public LatestQuote get(String symbol) {
        return latestBySymbol.get(symbol);
    }

    @Override
    public Map<String, LatestQuote> snapshot() {
        return Map.copyOf(latestBySymbol);
    }

    public void clear() {
        latestBySymbol.clear();
    }
}
