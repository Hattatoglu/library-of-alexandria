package dev.eyaz.lib.of.alex.service.auth.integration.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifies the explicit expiry check in RefreshTokenRepositoryIT.
 *
 * Refresh tokens are opaque (see ADR-001 addendum) and carry no claims of their own, unlike
 * a JWT whose exp claim is rejected automatically by the parser. Expiry must therefore be
 * checked explicitly against the persisted expires_at column — this test exists specifically
 * to guard that check, since a regression here would let expired tokens be rotated forever.
 *
 * @SpringBootTest (rather than @DataJpaTest) is required here because the behavior under
 * test lives in the adapter bean itself, not just the JPA repository.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepositoryIT — Integration Tests")
class RefreshTokenRepositoryIT {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("authdb_test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private RefreshTokenUseCasePersistenceTokenPort adapter;
    @Autowired private RefreshTokenRepository tokenRepository;
    @Autowired private UserAuthRepository userRepository;

    private UUID userId;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        userId = userRepository.save(buildUser()).getUserId();
    }

    @Test
    @DisplayName("A non-expired token is found and its userId is resolved")
    void shouldResolveUserIdForValidToken() {
        tokenRepository.save(buildToken("valid-token", userId, LocalDateTime.now().plusDays(7)));

        RefreshTokenUseCase result = adapter.findRefreshTokenByToken(buildUseCase("valid-token"));

        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("An expired token throws InvalidTokenException")
    void shouldThrowForExpiredToken() {
        tokenRepository.save(buildToken("expired-token", userId, LocalDateTime.now().minusSeconds(1)));

        System.out.println(tokenRepository.findByToken("expired-token"));

        assertThatThrownBy(() -> adapter.findRefreshTokenByToken(buildUseCase("expired-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("An expired token is deleted from the database when rejected")
    void shouldDeleteExpiredTokenOnRejection() {
        tokenRepository.save(buildToken("expired-token", userId, LocalDateTime.now().minusSeconds(1)));

        assertThatThrownBy(() -> adapter.findRefreshTokenByToken(buildUseCase("expired-token")));

        assertThat(tokenRepository.findByToken("expired-token")).isEmpty();
    }

    @Test
    @DisplayName("A token expiring exactly now is treated as expired")
    void shouldTreatTokenExpiringNowAsExpired() {
        // expiresAt strictly before "now" at check-time — emulate the boundary by using a
        // timestamp a moment in the past relative to when the adapter performs its check.
        tokenRepository.save(buildToken("boundary-token", userId, LocalDateTime.now().minusNanos(1)));

        assertThatThrownBy(() -> adapter.findRefreshTokenByToken(buildUseCase("boundary-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private RefreshTokenUseCase buildUseCase(String token) {
        RefreshTokenUseCase uc = new RefreshTokenUseCase();
        uc.setRefreshToken(token);
        return uc;
    }

    private RefreshTokenEntity buildToken(String token, UUID userId, LocalDateTime expiresAt) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setToken(token);
        e.setUserId(userId);
        e.setExpiresAt(expiresAt);
        return e;
    }

    private UserAuthEntity buildUser() {
        UserAuthEntity u = new UserAuthEntity();
        u.setName("Test User");
        u.setUserId(UUID.randomUUID());
        u.setUsername("testuser-" + UUID.randomUUID());
        u.setPassword("$2a$10$hashed");
        u.setEmail(UUID.randomUUID() + "@example.com");
        u.setRoles(new HashSet<>(Set.of(Role.ROLE_CUSTOM_USER)));
        return u;
    }
}
