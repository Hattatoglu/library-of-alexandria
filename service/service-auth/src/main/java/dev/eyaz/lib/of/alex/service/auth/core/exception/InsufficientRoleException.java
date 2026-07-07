package dev.eyaz.lib.of.alex.service.auth.core.exception;

public class InsufficientRoleException extends RuntimeException {
    public InsufficientRoleException(String message) {
        super(message);
    }
}
