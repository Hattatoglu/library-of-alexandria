package dev.eyaz.lib.of.alex.service.auth.core.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
