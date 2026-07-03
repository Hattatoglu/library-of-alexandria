package dev.eyaz.lib.of.alex.server.gateway.ratelimiter;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingWebFilter.class);

    private final RateLimitDecision rateLimitDecision;
    private final RateLimitProperties properties;
    private final GatewayMetrics gatewayMetrics;

    public RateLimitingWebFilter(
            RateLimitDecision rateLimitDecision,
            RateLimitProperties properties,
            GatewayMetrics gatewayMetrics
    ) {
        this.rateLimitDecision = rateLimitDecision;
        this.properties = properties;
        this.gatewayMetrics = gatewayMetrics;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String rateLimitKey = resolveRateLimitKey(exchange);

        return rateLimitDecision.isAllowed(rateLimitKey)
                .flatMap(allowed -> allowed
                        ? chain.filter(exchange)
                        : tooManyRequests(exchange, rateLimitKey));
    }

    /**
     * FR2: rate limits are keyed per user (JWT subject, forwarded as
     * X-User-Id by JwtValidationWebFilter). IP-based fallback is kept for
     * defensive reuse if this filter is ever applied ahead of, or on a
     * route without, JWT validation — not expected on the current chain
     * ordering, where JwtValidationWebFilter always runs first and rejects
     * unauthenticated requests before they reach here.
     */
    private String resolveRateLimitKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return userId;
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.toString() : "unknown";
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, String key) {
        log.debug("Rate limit exceeded: key={}, path={}", key, exchange.getRequest().getPath());
        gatewayMetrics.recordRateLimitRejection();
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", String.valueOf(properties.refillWindow().toSeconds()));
        return exchange.getResponse().setComplete();
    }
}
