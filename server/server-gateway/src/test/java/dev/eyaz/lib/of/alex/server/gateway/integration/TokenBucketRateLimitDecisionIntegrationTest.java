package dev.eyaz.lib.of.alex.server.gateway.integration;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter.RateLimitProperties;
import dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter.TokenBucketRateLimitDecision;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the REAL Redis Lua script (not mocked) to prove the token
 * bucket is actually atomic and enforces capacity correctly — the gap
 * flagged when the unit test list was first drafted (Mockito can't
 * meaningfully verify Lua script atomicity).
 */
@Testcontainers
class TokenBucketRateLimitDecisionIntegrationTest {

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private TokenBucketRateLimitDecision decision;
    private GatewayMetrics gatewayMetrics;

    @BeforeAll
    static void installBlockHound() {
        BlockHound.install();
    }

    @BeforeEach
    void setUp() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        ReactiveStringRedisTemplate redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
        gatewayMetrics = new GatewayMetrics(new SimpleMeterRegistry());
        RateLimitProperties properties = new RateLimitProperties(5, Duration.ofSeconds(60));
        decision = new TokenBucketRateLimitDecision(redisTemplate, properties, gatewayMetrics);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
        REDIS.stop();
    }

    @Test
    void allowsRequestsWithinCapacity() {
        String key = "user-" + System.nanoTime();

        // Capacity is 5 — the first 5 calls must all be allowed.
        Flux.range(1, 5)
                .concatMap(i -> decision.isAllowed(key))
                .as(StepVerifier::create)
                .expectNext(true, true, true, true, true)
                .verifyComplete();
    }

    @Test
    void deniesRequestOnceCapacityIsExceeded() {
        String key = "user-" + System.nanoTime();

        // Sequential (concatMap, not merge) is important: the whole point
        // is proving atomicity holds even without artificial parallelism —
        // if the Lua script were NOT atomic, this simpler sequential case
        // would still coincidentally pass, so see the concurrency test
        // below for the real atomicity proof.
        for (int i = 0; i < 5; i++) {
            decision.isAllowed(key).block(); // sequential setup, not the assertion itself
        }

        StepVerifier.create(decision.isAllowed(key))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void remainsAtomicUnderConcurrentRequests() {
        // The real point of this test: fire capacity*4 concurrent requests
        // at the same key. If INCR+EXPIRE+threshold-check were NOT atomic
        // (e.g. implemented as separate GET-then-INCR calls), a race would
        // let more than `capacity` requests through. With the Lua script,
        // exactly `capacity` must be allowed, no matter the concurrency.
        String key = "user-" + System.nanoTime();
        int capacity = 5;
        int totalRequests = capacity * 4;

        long allowedCount = Flux.range(0, totalRequests)
                .flatMap(i -> decision.isAllowed(key)) // flatMap = concurrent, unlike concatMap above
                .filter(allowed -> allowed)
                .count()
                .block();

        assertThat(allowedCount).isEqualTo(capacity);
    }

    @Test
    void failsOpenWhenRedisIsUnreachable() {
        // A connection factory pointed at a port nothing is listening on —
        // simulates Redis being down without needing to stop/restart the
        // container mid-test.
        LettuceConnectionFactory brokenFactory = new LettuceConnectionFactory("localhost", 1);
        brokenFactory.afterPropertiesSet();
        ReactiveStringRedisTemplate brokenTemplate = new ReactiveStringRedisTemplate(brokenFactory);
        RateLimitProperties properties = new RateLimitProperties(5, Duration.ofSeconds(60));
        TokenBucketRateLimitDecision brokenDecision = new TokenBucketRateLimitDecision(brokenTemplate, properties, gatewayMetrics);

        StepVerifier.create(brokenDecision.isAllowed("any-key"))
                .expectNext(true) // fails OPEN per ADR-013 — request is allowed through
                .verifyComplete();

        brokenFactory.destroy();
    }
}