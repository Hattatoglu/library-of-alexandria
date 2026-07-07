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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UserAuthRepository — Integration Tests")
class UserAuthRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
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

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("User is saved and found by username")
    void shouldSaveAndFindByUsername() {
        repository.save(buildEntity("testuser", "testuser@example.com"));

        Optional<UserAuthEntity> result = repository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    @DisplayName("User is found by userId")
    void shouldFindByUserId() {
        UserAuthEntity saved = repository.save(buildEntity("testuser", "testuser@example.com"));

        Optional<UserAuthEntity> result = repository.findByUserId(saved.getUserId());

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Non-existent username returns Optional.empty()")
    void shouldReturnEmptyForUnknownUsername() {
        Optional<UserAuthEntity> result = repository.findByUsername("ghost");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Two records with the same username throw a DataIntegrityViolationException")
    void shouldEnforceUniqueUsername() {
        repository.save(buildEntity("testuser", "testuser@example.com"));

        UserAuthEntity duplicate = buildEntity("testuser", "other@example.com");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Two records with the same email throw a DataIntegrityViolationException")
    void shouldEnforceUniqueEmail() {
        repository.save(buildEntity("testuser", "testuser@example.com"));

        UserAuthEntity duplicate = buildEntity("emre2", "testuser@example.com");

        assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Role is added to the user and persisted")
    void shouldPersistRoles() {
        UserAuthEntity entity = buildEntity("testuser", "testuser@example.com");
        entity.getRoles().add(Role.ROLE_ADMIN_USER);
        UserAuthEntity saved = repository.save(entity);

        Optional<UserAuthEntity> found = repository.findByUsername("testuser");

        assertThat(found).isPresent();
        assertThat(found.get().getRoles()).contains(Role.ROLE_CUSTOM_USER, Role.ROLE_ADMIN_USER);
    }

    private UserAuthEntity buildEntity(String username, String email) {
        UserAuthEntity e = new UserAuthEntity();
        e.setName("Test User");
        e.setUserId(UUID.randomUUID());
        e.setUsername(username);
        e.setPassword("$2a$10$hashedpassword");
        e.setEmail(email);
        e.setRoles(new HashSet<>(java.util.Set.of(Role.ROLE_CUSTOM_USER)));
        return e;
    }
}
