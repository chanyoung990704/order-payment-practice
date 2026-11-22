package com.practice.order.quote.controller;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import com.practice.order.quote.consumer.QuoteCache;
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
    private final QuoteCache quoteCache;

    public QuoteController(QuoteProducer quoteProducer, QuoteCache quoteCache) {
        this.quoteProducer = quoteProducer;
        this.quoteCache = quoteCache;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> publish(@RequestBody QuotePublishRequest request) {
        quoteProducer.publish(QuoteEvent.of(
                request.symbol(), request.price(), request.volume(), request.sequence()));
        return ResponseEntity.accepted().body(Map.of("status", "PUBLISHED"));
    }

    @GetMapping("/{symbol}")
    public LatestQuote latest(@PathVariable String symbol) {
        return quoteCache.get(symbol);
    }

    @GetMapping
    public Map<String, LatestQuote> snapshot() {
        return quoteCache.snapshot();
    }

    public record QuotePublishRequest(
            @NotBlank String symbol,
            @Min(1) long price,
            @Min(0) long volume,
            long sequence
    ) {
    }
}
