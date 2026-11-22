package com.practice.order.quote.controller;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import com.practice.order.quote.store.QuoteStore;
import org.springframework.beans.factory.annotation.Qualifier;
import com.practice.order.quote.producer.QuoteProducer;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final QuoteProducer quoteProducer;
    private final QuoteStore quoteStore;

    public QuoteController(QuoteProducer quoteProducer,
                           @Qualifier("localQuoteStore") QuoteStore quoteStore) {
        this.quoteProducer = quoteProducer;
        this.quoteStore = quoteStore;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> publish(@RequestBody QuotePublishRequest request) {
        quoteProducer.publish(QuoteEvent.of(
                request.symbol(), request.price(), request.volume(), request.sequence()));
        return ResponseEntity.accepted().body(Map.of("status", "PUBLISHED"));
    }

    @GetMapping("/{symbol}")
    public LatestQuote latest(@PathVariable String symbol) {
        return quoteStore.get(symbol);
    }

    @GetMapping
    public Map<String, LatestQuote> snapshot() {
        return quoteStore.snapshot();
    }

    public record QuotePublishRequest(
            @NotBlank String symbol,
            @Min(1) long price,
            @Min(0) long volume,
            long sequence
    ) {
    }
}
