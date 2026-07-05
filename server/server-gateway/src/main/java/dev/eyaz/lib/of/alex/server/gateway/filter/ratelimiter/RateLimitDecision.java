package dev.eyaz.lib.of.alex.server.gateway.filter.ratelimiter;

import reactor.core.publisher.Mono;

public interface RateLimitDecision {

    Mono<Boolean> isAllowed(String key);
}
