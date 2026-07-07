package dev.eyaz.lib.of.alex.server.gateway.exception.exceptions;

import org.springframework.http.HttpStatus;

public class JwtValidationException extends GatewayException {

    private final String reasonCode;

    public JwtValidationException(String reasonCode) {
        super("Access token validation failed: " + reasonCode);
        this.reasonCode = reasonCode;
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String errorCode() {
        return reasonCode;
    }
}
