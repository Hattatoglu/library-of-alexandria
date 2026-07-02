package dev.eyaz.lib.of.alex.server.gateway.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @Order(-2) places this ahead of Spring Boot's DefaultErrorWebExceptionHandler
 * (registered at order -1), so it fully replaces the default error page /
 * default JSON error body behavior for this application.
 */
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    private final ObjectMapper objectMapper;
    private final GatewayMetrics gatewayMetrics;

    public GlobalErrorWebExceptionHandler(ObjectMapper objectMapper, GatewayMetrics gatewayMetrics) {
        this.objectMapper = objectMapper;
        this.gatewayMetrics = gatewayMetrics;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status;
        String errorCode;

        if (ex instanceof GatewayException gatewayException) {
            status = gatewayException.httpStatus();
            errorCode = gatewayException.errorCode();
            // Expected, already-classified failure — WARN is enough, no stack trace noise.
            log.warn("Gateway request rejected: errorCode={}, path={}",
                    errorCode, exchange.getRequest().getPath(), ex);
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "internal_error";
            // Unexpected — this is exactly the case where the correlation ID
            // (see CorrelationIdWebFilter/LoggingConfig) earns its keep: it's
            // what lets this ERROR log line be tied back to the client's report.
            log.error("Unhandled exception in Gateway, path={}", exchange.getRequest().getPath(), ex);
        }

        ErrorResponse body = ErrorResponse.of(
                status.value(),
                errorCode,
                safeMessage(ex),
                exchange.getRequest().getPath().value(),
                MDC.get("correlationId")
        );

        gatewayMetrics.recordError(errorCode, status.value());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.fromSupplier(() -> {
            byte[] bytes = writeAsBytes(body);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return buffer;
        }));
    }

    private byte[] writeAsBytes(ErrorResponse body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception serializationFailure) {
            // Last-resort fallback if even the error body fails to serialize —
            // must not throw from inside an error handler.
            log.error("Failed to serialize ErrorResponse", serializationFailure);
            return ("{\"errorCode\":\"internal_error\",\"message\":\"error serialization failed\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private String safeMessage(Throwable ex) {
        // Never leak internal exception details (SQL fragments, stack internals,
        // library-specific messages) for unexpected exceptions — only
        // GatewayException subtypes are trusted to have a client-safe message.
        if (ex instanceof GatewayException) {
            return ex.getMessage();
        }
        return "An unexpected error occurred.";
    }
}
