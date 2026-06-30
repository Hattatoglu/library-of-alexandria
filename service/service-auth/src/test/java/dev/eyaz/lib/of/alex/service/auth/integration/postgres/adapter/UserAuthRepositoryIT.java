package dev.eyaz.lib.of.alex.service.auth.integration.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
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

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserAuthRepository — Integration Tests")
class UserAuthRepositoryIT {

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

    @Autowired
    private UserAuthRepository repository;
    private UUID userId = UUID.fromString("e0df422f-c7df-4b7a-9f12-9a6ca58455a9");

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Kullanıcı kaydedilir ve username ile bulunur")
    void shouldSaveAndFindByUsername() {
        repository.save(buildEntity("emre", "emre@example.com"));

        Optional<UserAuthEntity> result = repository.findByUsername("emre");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("emre@example.com");
    }

    @Test
    @DisplayName("Kullanıcı userId ile bulunur")
    void shouldFindByUserId() {
        UserAuthEntity saved = repository.save(buildEntity("emre", "emre@example.com"));

        Optional<UserAuthEntity> result = repository.findByUserId(saved.getUserId());

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("emre");
    }

    @Test
    @DisplayName("Mevcut olmayan username → Optional.empty()")
    void shouldReturnEmptyForUnknownUsername() {
        Optional<UserAuthEntity> result = repository.findByUsername("ghost");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Aynı username ile iki kayıt → DataIntegrityViolationException")
    void shouldEnforceUniqueUsername() {
        repository.save(buildEntity("emre", "emre@example.com"));

        UserAuthEntity duplicate = buildEntity("emre", "other@example.com");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Aynı email ile iki kayıt → DataIntegrityViolationException")
    void shouldEnforceUniqueEmail() {
        repository.save(buildEntity("emre", "emre@example.com"));

        UserAuthEntity duplicate = buildEntity("emre2", "emre@example.com");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

//    @Test
//    @DisplayName("Kaydedilen kullanıcıda varsayılan account flag'leri true gelir")
//    void shouldHaveDefaultAccountFlags() {
//        UserAuthEntity saved = repository.save(buildEntity("emre", "emre@example.com"));
//
//        assertThat(saved.isAccountNonExpired()).isTrue();
//        assertThat(saved.isAccountNonLocked()).isTrue();
//        assertThat(saved.isCredentialsNonExpired()).isTrue();
//        assertThat(saved.isEnabled()).isTrue();
//    }

    @Test
    @DisplayName("Kullanıcıya rol eklenir ve persist edilir")
    void shouldPersistRoles() {
        UserAuthEntity entity = buildEntity("emre", "emre@example.com");
        entity.getRoles().add(Role.ROLE_ADMIN_USER);
        UserAuthEntity saved = repository.save(entity);

        Optional<UserAuthEntity> found = repository.findByUsername("emre");

        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).contains(Role.ROLE_CUSTOM_USER, Role.ROLE_ADMIN_USER);
    }

    private UserAuthEntity buildEntity(String username, String email) {
        UserAuthEntity e = new UserAuthEntity();
        e.setName("Test User");
        e.setUsername(username);
        e.setUserId(userId);
        e.setPassword("$2a$10$hashedpassword");
        e.setEmail(email);
        e.setRoles(new HashSet<>(java.util.Set.of(Role.ROLE_CUSTOM_USER)));
        return e;
    }
}
