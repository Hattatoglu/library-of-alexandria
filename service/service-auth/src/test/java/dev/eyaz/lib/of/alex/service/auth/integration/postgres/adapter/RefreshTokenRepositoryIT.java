package dev.eyaz.lib.of.alex.service.auth.integration.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.RefreshTokenEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("RefreshTokenRepository — Integration Tests")
class RefreshTokenRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("authdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private RefreshTokenRepository tokenRepository;
    @Autowired private UserAuthRepository userRepository;

    private UserAuthEntity savedUser;
    private UUID userId = UUID.fromString("e0df422f-c7df-4b7a-9f12-9a6ca58455a9");

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        savedUser = userRepository.save(buildUser());
    }

    @Test
    @DisplayName("Refresh token kaydedilir ve token string ile bulunur")
    void shouldSaveAndFindByToken() {
        tokenRepository.save(buildToken("token-abc", savedUser.getUserId(), future()));

        Optional<RefreshTokenEntity> found = tokenRepository.findByToken("token-abc");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo(savedUser.getUserId());
    }

    @Test
    @DisplayName("Mevcut olmayan token → Optional.empty()")
    void shouldReturnEmptyForUnknownToken() {
        Optional<RefreshTokenEntity> found = tokenRepository.findByToken("non-existent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Token silinir ve artık bulunamaz")
    void shouldDeleteByToken() {
        tokenRepository.save(buildToken("token-to-delete", savedUser.getUserId(), future()));

        tokenRepository.deleteByToken("token-to-delete");

        assertThat(tokenRepository.findByToken("token-to-delete")).isEmpty();
    }

//    @Test
//    @DisplayName("UserId ile kullanıcının tüm token'ları silinir")
//    void shouldDeleteAllTokensByUserId() {
//        tokenRepository.save(buildToken("token-1", savedUser.getUserId(), future()));
//        tokenRepository.save(buildToken("token-2", savedUser.getUserId(), future()));
//
//        tokenRepository.deleteByUserId(savedUser.getUserId());
//
//        assertThat(tokenRepository.findByToken("token-1")).isEmpty();
//        assertThat(tokenRepository.findByToken("token-2")).isEmpty();
//    }

    @Test
    @DisplayName("Aynı token string iki kez kaydedilmeye çalışılırsa hata fırlatılır")
    void shouldEnforceUniqueToken() {
        tokenRepository.save(buildToken("duplicate-token", savedUser.getUserId(), future()));

        RefreshTokenEntity duplicate = buildToken("duplicate-token", savedUser.getUserId(), future());

        assertThatThrownBy(() -> tokenRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private LocalDateTime future() {
        return LocalDateTime.now().plusDays(7);
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
        u.setName("Test");
        u.setUserId(userId);
        u.setUsername("test");
        u.setPassword("$2a$10$hashed");
        u.setEmail("test@main.com");
        u.setRoles(new HashSet<>(Set.of(Role.ROLE_CUSTOM_USER)));
        return u;
    }
}
