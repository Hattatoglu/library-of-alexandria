package dev.eyaz.lib.of.alex.server.gateway.exception;

import java.time.Instant;

/**
 * Consistent error body returned by the Gateway for every failure path
 * (auth rejection, rate limit, circuit open, unexpected exception) so
 * clients always parse the same shape regardless of which filter produced
 * the error.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path,
        String correlationId
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path, String correlationId) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, path, correlationId);
    }
}
