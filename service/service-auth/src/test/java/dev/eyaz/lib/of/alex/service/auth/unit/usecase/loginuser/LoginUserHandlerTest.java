package dev.eyaz.lib.of.alex.service.auth.unit.usecase.loginuser;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUserHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserRefreshTokenPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserSecurityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginUserHandler — Unit Tests")
class LoginUserHandlerTest {

    // ── Fakes ────────────────────────────────────────────────────────────────
    private static class FakeLoginUserSecurityPort implements LoginUserSecurityPort {
        @Override
        public LoginUser generateAccessAndRefreshTokens(LoginUser usecase) {
            usecase.setAccessToken("access-token-stub");
            usecase.setRefreshToken("refresh-token-stub");
            usecase.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
            usecase.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
            return usecase;
        }
    }

    private static class FakeLoginUserRefreshTokenPort implements LoginUserRefreshTokenPersistenceTokenPort {
        private LoginUser persisted;

        @Override
        public LoginUser persistRefreshToken(LoginUser usecase) {
            this.persisted = usecase;
            return usecase;
        }

        LoginUser getPersisted() { return persisted; }
    }

    private FakeLoginUserSecurityPort fakeSecurityPort;
    private FakeLoginUserRefreshTokenPort fakeTokenPort;
    private LoginUserHandler handler;

    @BeforeEach
    void setUp() {
        fakeSecurityPort = new FakeLoginUserSecurityPort();
        fakeTokenPort    = new FakeLoginUserRefreshTokenPort();
        handler          = new LoginUserHandler(fakeSecurityPort, fakeTokenPort);
    }

    @Test
    @DisplayName("Login sonrası access ve refresh token üretilir")
    void shouldGenerateBothTokens() {
        LoginUser result = handler.handle(buildInput());

        assertThat(result.getAccessToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotBlank();
    }

    @Test
    @DisplayName("Refresh token persistence port'a iletilir")
    void shouldPersistRefreshToken() {
        handler.handle(buildInput());

        assertThat(fakeTokenPort.getPersisted()).isNotNull();
        assertThat(fakeTokenPort.getPersisted().getRefreshToken()).isEqualTo("refresh-token-stub");
    }

    @Test
    @DisplayName("Token expiry tarihleri set edilir")
    void shouldSetTokenExpiries() {
        LoginUser result = handler.handle(buildInput());

        assertThat(result.getAccessTokenExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(result.getRefreshTokenExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Token'lar birbirinden farklıdır")
    void accessAndRefreshTokensShouldBeDifferent() {
        LoginUser result = handler.handle(buildInput());

        assertThat(result.getAccessToken()).isNotEqualTo(result.getRefreshToken());
    }

    private LoginUser buildInput() {
        LoginUser u = new LoginUser();
        u.setUsername("emre");
        u.setUserId(UUID.randomUUID());
        u.setRole(Set.of(Role.ROLE_CUSTOM_USER));
        return u;
    }
}
