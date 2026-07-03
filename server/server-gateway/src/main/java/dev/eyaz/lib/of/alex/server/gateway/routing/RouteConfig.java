package dev.eyaz.lib.of.alex.server.gateway.routing;

/**
 * A single routing rule (FR1): requests whose path starts with pathPrefix
 * are forwarded to baseUrl, and protected by the circuit breaker instance
 * named serviceName (must match a resilience4j.circuitbreaker.instances.*
 * entry in application.yml).
 *
 * ADR-012: single static baseUrl and service-level circuit breaker is the
 * deliberate CURRENT-STATE design — each downstream service runs as a
 * single instance today, so a per-instance load balancer/circuit breaker
 * would be complexity with nothing to gain yet. When a service is actually
 * deployed with multiple instances, ADR-012's documented future-state
 * change applies: this becomes a list of addresses, and the circuit
 * breaker moves to per-instance granularity.
 */
public record RouteConfig(
        String pathPrefix,
        String serviceName,
        String baseUrl
) {
}

