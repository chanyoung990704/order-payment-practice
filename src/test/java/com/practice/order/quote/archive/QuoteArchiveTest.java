package com.practice.order.quote.archive;

import com.practice.order.quote.domain.LatestQuote;
import com.practice.order.quote.domain.QuoteEvent;
import com.practice.order.quote.store.LocalQuoteCache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteArchiveTest {

    @Test
    void 같은_시퀀스의_중복_틱은_원본에_한_번만_적재된다() {
        QuoteArchiveConsumer archive = new QuoteArchiveConsumer();
        QuoteEvent event = QuoteEvent.of("005930", 70_000, 100, 1);

        archive.archive(event);
        archive.archive(event);

        assertThat(archive.archivedCount()).isEqualTo(1);
    }

    @Test
    void 다른_시퀀스는_각각_원본으로_남는다() {
        QuoteArchiveConsumer archive = new QuoteArchiveConsumer();

        archive.archive(QuoteEvent.of("005930", 70_000, 100, 1));
        archive.archive(QuoteEvent.of("005930", 70_100, 120, 2));

        assertThat(archive.archivedCount()).isEqualTo(2);
    }

    @Test
    void 스냅샷은_현재_최신가를_그대로_보존한다() {
        LocalQuoteCache cache = new LocalQuoteCache();
        QuoteSnapshotWriter writer = new QuoteSnapshotWriter(cache);
        cache.update(QuoteEvent.of("005930", 70_000, 100, 1));

        writer.writeSnapshot();
        cache.update(QuoteEvent.of("005930", 71_000, 200, 2));

        assertThat(writer.snapshotCount()).isEqualTo(1);
        assertThat(cache.get("005930").price()).isEqualTo(71_000);
    }
}
