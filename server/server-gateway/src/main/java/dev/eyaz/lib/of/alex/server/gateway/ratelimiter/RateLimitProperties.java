package dev.eyaz.lib.of.alex.server.gateway.ratelimiter;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway.ratelimit")
public record RateLimitProperties(
        int bucketCapacity,
        Duration refillWindow
) {
}
