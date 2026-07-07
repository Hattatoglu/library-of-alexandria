package dev.eyaz.lib.of.alex.server.gateway.exception.handler;

import java.time.Instant;

/**
 * Consistent error body returned by the Gateway for every failure path
 * (auth rejection, rate limit, circuit open, unexpected exception) so
 * clients always parse the same shape regardless of which filter produced
 * the error.
 */
public record ErrorResponse(
        String timestamp,
        int status,
        String errorCode,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String errorCode, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status, errorCode, message, path);
    }
}
