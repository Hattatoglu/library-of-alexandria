package dev.eyaz.lib.of.alex.service.auth.infra.rest.exception;

import dev.eyaz.lib.of.alex.artifactory.lib.infra.exception.WebExceptionHandlerSupport;
import dev.eyaz.lib.of.alex.service.auth.core.exception.*;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ServiceAuthExceptionHandler extends WebExceptionHandlerSupport {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthExceptionHandler.class);
    private final AuthMetrics authMetrics;

    public ServiceAuthExceptionHandler(AuthMetrics authMetrics) {
        this.authMetrics = authMetrics;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("action=request_rejected type=user_already_exists detail={}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "user-already-exists", ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("action=request_rejected type=user_not_found detail={}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "user-not-found", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        authMetrics.incrementLoginFailureInvalidCredentials();
        log.warn("action=request_rejected type=invalid_credentials detail={}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException ex) {
        log.warn("action=request_rejected type=invalid_token detail={}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "invalid-token", ex.getMessage());
    }

    @ExceptionHandler(InsufficientRoleException.class)
    public ProblemDetail handleInsufficientRole(InsufficientRoleException ex) {
        log.warn("action=request_rejected type=insufficient_role detail={}", ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "insufficient-role", ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        authMetrics.incrementLoginFailureInvalidCredentials();
        log.warn("action=request_rejected type=bad_credentials detail={}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage());
    }
}
