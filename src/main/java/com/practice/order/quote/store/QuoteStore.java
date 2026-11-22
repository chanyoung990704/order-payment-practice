package com.practice.order.quote.store;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;

import java.util.Map;

public interface QuoteStore {

    LatestQuote update(QuoteEvent event);

    LatestQuote get(String symbol);

    Map<String, LatestQuote> snapshot();
}
