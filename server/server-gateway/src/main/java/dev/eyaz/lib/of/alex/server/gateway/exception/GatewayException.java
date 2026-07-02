package dev.eyaz.lib.of.alex.server.gateway.exception;

import org.springframework.http.HttpStatus;

public abstract class GatewayException extends RuntimeException {

    protected GatewayException(String message) {
        super(message);
    }

    public abstract HttpStatus httpStatus();

    public abstract String errorCode();
}

