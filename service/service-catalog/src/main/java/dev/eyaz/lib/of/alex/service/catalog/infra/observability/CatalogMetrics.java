package dev.eyaz.lib.of.alex.service.catalog.infra.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class CatalogMetrics {
    // ── Counters: add book ───────────────────────────────────────────────────
    private final Counter addBookSuccess;

    public CatalogMetrics(MeterRegistry registry) {

        //Add Book
        this.addBookSuccess = Counter.builder("catalog.addbook.success")
                .description("Number of successful adding book attempts")
                .register(registry);
    }

    // ── Counters: add book ───────────────────────────────────────────────────
    public void incrementAddBookSuccess() { addBookSuccess.increment();}
}
