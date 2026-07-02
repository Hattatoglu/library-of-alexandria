package dev.eyaz.lib.of.alex.server.gateway.exception.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Raised when the RSA public key cannot be obtained from ServiceAuth (see
 * PublicKeyProvider). Deliberately distinct from JwtValidationException:
 * this is not a problem with the client's token — it's the Gateway's own
 * dependency being unavailable — so it maps to 503 Service Unavailable,
 * not 401 Unauthorized. JwtValidationWebFilter's onErrorResume only
 * intercepts JwtValidationException, so this propagates past it to
 * GlobalErrorWebExceptionHandler, which maps any GatewayException to its
 * own httpStatus()/errorCode() and records it via GatewayMetrics.recordError.
 */
public class PublicKeyUnavailableException extends GatewayException {

    public PublicKeyUnavailableException(Throwable cause) {
        super("Unable to obtain RSA public key from ServiceAuth: " + cause.getMessage());
        initCause(cause);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }

    @Override
    public String errorCode() {
        return "auth_service_unavailable";
    }
}
