package dev.eyaz.lib.of.alex.service.auth.unit.usecase.logoutuser;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler.LogoutUserHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port.LogoutUserPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogoutUserHandler — Unit Tests")
class LogoutUserHandlerTest {

    private static class FakeLogoutPort implements LogoutUserPersistenceTokenPort {
        private LogoutUser received;

        @Override
        public LogoutUser deleteRefreshTokenByToken(LogoutUser useCase) {
            this.received = useCase;
            return useCase;
        }

        LogoutUser getReceived() { return received; }
    }

    private FakeLogoutPort fakePort;
    private LogoutUserHandler handler;
    private AuthMetrics authMetrics = new AuthMetrics(new SimpleMeterRegistry());

    @BeforeEach
    void setUp() {
        fakePort = new FakeLogoutPort();
        handler  = new LogoutUserHandler(fakePort, authMetrics);
    }

    @Test
    @DisplayName("Logout isteği token persistence port'a iletilir")
    void shouldDelegateToPort() {
        LogoutUser input = buildInput("my-refresh-token");

        handler.handle(input);

        assertThat(fakePort.getReceived()).isNotNull();
        assertThat(fakePort.getReceived().getRefreshToken()).isEqualTo("my-refresh-token");
    }

    @Test
    @DisplayName("Handler port'tan dönen nesneyi değiştirmeden geri döndürür")
    void shouldReturnPortResult() {
        LogoutUser input  = buildInput("token-xyz");
        LogoutUser result = handler.handle(input);

        assertThat(result.getRefreshToken()).isEqualTo("token-xyz");
    }

    private LogoutUser buildInput(String token) {
        LogoutUser u = new LogoutUser();
        u.setUserId(UUID.randomUUID());
        u.setRefreshToken(token);
        return u;
    }
}
