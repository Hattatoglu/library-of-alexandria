package dev.eyaz.lib.of.alex.server.gateway.exception.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Raised when Resilience4j's CircuitBreakerOperator rejects a call because
 * the circuit is OPEN (wraps io.github.resilience4j.circuitbreaker.CallNotPermittedException).
 * No filter-specific response shaping is needed beyond status + errorCode,
 * so — unlike JwtValidationWebFilter's inline handling — this is left to
 * propagate to GlobalErrorWebExceptionHandler, which already logs and
 * records it via GatewayMetrics.recordError.
 */
public class CircuitOpenException extends GatewayException {

    public CircuitOpenException(String serviceName) {
        super("Circuit breaker is open for service: " + serviceName);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    @Override
    public String errorCode() {
        return "circuit_open";
    }
}


