package com.practice.order.quote.archive;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.store.QuoteStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuoteSnapshotWriter {

    private final QuoteStore quoteStore;
    private final Map<Instant, Map<String, LatestQuote>> snapshots = new ConcurrentHashMap<>();

    public QuoteSnapshotWriter(@Qualifier("localQuoteStore") QuoteStore quoteStore) {
        this.quoteStore = quoteStore;
    }

    @Scheduled(fixedDelayString = "${quote.snapshot.interval-ms:5000}")
    public void writeSnapshot() {
        snapshots.put(Instant.now(), quoteStore.snapshot());
    }

    public int snapshotCount() {
        return snapshots.size();
    }
}
