package dev.eyaz.lib.of.alex.service.auth.integration.rest.api;

import dev.eyaz.lib.of.alex.service.auth.core.exception.*;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.exception.ServiceAuthExceptionHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceAuthExceptionHandler — Unit Tests")
class ServiceAuthExceptionHandlerTest {

    private ServiceAuthExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ServiceAuthExceptionHandler(new AuthMetrics(new SimpleMeterRegistry()));
    }

    @Test
    @DisplayName("UserAlreadyExistsException → 409 CONFLICT + ProblemDetail")
    void shouldReturn409ForUserAlreadyExists() {
        ProblemDetail pd = handler.handleUserAlreadyExists(
                new UserAlreadyExistsException("Username already taken: emre"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getDetail()).contains("emre");
        assertThat(pd.getType().toString()).contains("user-already-exists");
    }

    @Test
    @DisplayName("UserNotFoundException → 404 NOT_FOUND + ProblemDetail")
    void shouldReturn404ForUserNotFound() {
        ProblemDetail pd = handler.handleUserNotFound(
                new UserNotFoundException("User not found: ghost"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).contains("ghost");
        assertThat(pd.getType().toString()).contains("user-not-found");
    }

    @Test
    @DisplayName("InvalidCredentialsException → 401 UNAUTHORIZED + ProblemDetail")
    void shouldReturn401ForInvalidCredentials() {
        ProblemDetail pd = handler.handleInvalidCredentials(
                new InvalidCredentialsException("Bad credentials"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getType().toString()).contains("invalid-credentials");
    }

    @Test
    @DisplayName("InvalidTokenException → 401 UNAUTHORIZED + ProblemDetail")
    void shouldReturn401ForInvalidToken() {
        ProblemDetail pd = handler.handleInvalidToken(
                new InvalidTokenException("Token not found"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getType().toString()).contains("invalid-token");
    }

    @Test
    @DisplayName("InsufficientRoleException → 403 FORBIDDEN + ProblemDetail")
    void shouldReturn403ForInsufficientRole() {
        ProblemDetail pd = handler.handleInsufficientRole(
                new InsufficientRoleException("Access denied"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(pd.getType().toString()).contains("insufficient-role");
    }

    @Test
    @DisplayName("ProblemDetail timestamp property her zaman set edilir")
    void shouldAlwaysIncludeTimestamp() {
        ProblemDetail pd = handler.handleUserNotFound(
                new UserNotFoundException("test"));

        assertThat(pd.getProperties()).containsKey("timestamp");
        assertThat(pd.getProperties().get("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("IllegalArgumentException → 400 BAD_REQUEST (parent handler)")
    void shouldReturn400ForIllegalArgument() {
        ProblemDetail pd = handler.handleIllegalArgument(
                new IllegalArgumentException("No cookies present in request"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).contains("cookies");
    }
}
