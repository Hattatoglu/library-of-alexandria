package dev.eyaz.lib.of.alex.server.gateway.exception.exceptions;

import org.springframework.http.HttpStatus;

/*
 * Raised when the circuit is CLOSED/HALF_OPEN (the call was permitted) but
 * the actual downstream call failed (connection refused, timeout, DNS
 * failure). Distinct from CircuitOpenException: this is the Gateway's
 * proxy call itself failing, not the circuit breaker rejecting the attempt
 * — 502 Bad Gateway, not 503. Resilience4j's CircuitBreakerOperator still
 * records this failure against the circuit breaker's own state (that
 * happens automatically — see RoutingWebFilter), independent of this
 * exception's job of shaping the client-facing response.
 */
public class DownstreamUnavailableException extends GatewayException {

    public DownstreamUnavailableException(String serviceName, Throwable cause) {
        super("Downstream call failed for service: " + serviceName + " (" + cause.getMessage() + ")");
        initCause(cause);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }

    @Override
    public String errorCode() {
        return "downstream_unavailable";
    }
}

