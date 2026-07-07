package dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class TokenBucketRateLimitDecision implements RateLimitDecision {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimitDecision.class);

    // KEYS[1] = bucket key, ARGV[1] = capacity, ARGV[2] = ttl seconds.
    // Returns 1 if the request is allowed, 0 if the bucket is exhausted.
    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            else
                return 1
            end
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final GatewayMetrics gatewayMetrics;

    public TokenBucketRateLimitDecision(
            ReactiveStringRedisTemplate redisTemplate,
            RateLimitProperties properties,
            GatewayMetrics gatewayMetrics
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.gatewayMetrics = gatewayMetrics;
    }

    @Override
    public Mono<Boolean> isAllowed(String key) {
        String bucketKey = "rate-limit:" + key;

        return redisTemplate.execute(
                        TOKEN_BUCKET_SCRIPT,
                        List.of(bucketKey),
                        List.of(String.valueOf(properties.bucketCapacity()), String.valueOf(properties.refillWindow().toSeconds()))
                )
                .next() // execute() returns a Flux<Long> for a single-key script; next() unwraps it
                .map(result -> result == 1L)
                .onErrorResume(redisUnavailable -> {
                    log.warn("Rate limiter failing open — Redis unavailable for key={}", key, redisUnavailable);
                    gatewayMetrics.recordRateLimitFailOpen();
                    return Mono.just(true);
                });
    }
}
