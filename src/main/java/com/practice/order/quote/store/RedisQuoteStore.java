package com.practice.order.quote.store;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("redisQuoteStore")
public class RedisQuoteStore implements QuoteStore {

    private static final String KEY_PREFIX = "quote:latest:";

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> compareAndSetScript;

    public RedisQuoteStore(RedisTemplate<String, String> redisTemplate,
                           @Value("${quote.redis.enabled:true}") boolean enabled) {
        this.redisTemplate = redisTemplate;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/quote-compare-and-set.lua")));
        script.setResultType(Long.class);
        this.compareAndSetScript = script;
    }

    @Override
    public LatestQuote update(QuoteEvent event) {
        LatestQuote candidate = LatestQuote.from(event);
        redisTemplate.execute(compareAndSetScript,
                List.of(KEY_PREFIX + event.symbol()),
                String.valueOf(candidate.sequence()),
                String.valueOf(candidate.price()),
                String.valueOf(candidate.volume()),
                String.valueOf(candidate.updatedAtEpochMilli()));
        return candidate;
    }

    @Override
    public LatestQuote get(String symbol) {
        Map<Object, Object> fields = redisTemplate.opsForHash()
                .entries(KEY_PREFIX + symbol);
        if (fields.isEmpty()) {
            return null;
        }
        return new LatestQuote(
                symbol,
                Long.parseLong(String.valueOf(fields.get("price"))),
                Long.parseLong(String.valueOf(fields.get("volume"))),
                Long.parseLong(String.valueOf(fields.get("seq"))),
                Long.parseLong(String.valueOf(fields.get("ts")))
        );
    }

    @Override
    public Map<String, LatestQuote> snapshot() {
        return redisTemplate.keys(KEY_PREFIX + "*").stream()
                .map(key -> get(key.substring(KEY_PREFIX.length())))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        LatestQuote::symbol, quote -> quote));
    }
}
