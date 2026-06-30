package dev.eyaz.lib.of.alex.service.auth.unit.usecase.createuser;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserAlreadyExistsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUserHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port.SignUpUserPersistenceAuthPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DisplayName("CreateUserHandler — Unit Tests")
class CreateUserHandlerTest {

    // ── Fake port — no mocks needed ─────────────────────────────────────────
    private static class FakeSignUpUserPersistenceAuthPort implements SignUpUserPersistenceAuthPort {

        private final Set<String> existingUsernames;
        private final Set<String> existingEmails;
        private SignUpUser savedUser;

        FakeSignUpUserPersistenceAuthPort(Set<String> existingUsernames, Set<String> existingEmails) {
            this.existingUsernames = existingUsernames;
            this.existingEmails    = existingEmails;
        }

        @Override
        public SignUpUser checkUsernameAndMail(SignUpUser usecase) {
            if (existingUsernames.contains(usecase.getUsername())) {
                throw new UserAlreadyExistsException("Username already taken: " + usecase.getUsername());
            }
            if (existingEmails.contains(usecase.getEmail())) {
                throw new UserAlreadyExistsException("Email already registered: " + usecase.getEmail());
            }
            return usecase;
        }

        @Override
        public SignUpUser saveUser(SignUpUser usecase) {
            this.savedUser = usecase;
            return usecase;
        }

        SignUpUser getSavedUser() { return savedUser; }
    }

    private FakeSignUpUserPersistenceAuthPort fakePort;
    private SignUpUserHandler handler;

    @BeforeEach
    void setUp() {
        fakePort = new FakeSignUpUserPersistenceAuthPort(new HashSet<>(), new HashSet<>());
        handler  = new SignUpUserHandler(fakePort);
    }

    // ── Happy path ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Yeni kullanıcı başarıyla kaydedilir ve userId atanır")
    void shouldCreateUserSuccessfully() {
        SignUpUser input = buildInput("emre", "emre@example.com");

        SignUpUser result = handler.handle(input);

        assertThat(result.getUserId()).isNotNull();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Yeni kullanıcıya varsayılan olarak ROLE_CUSTOM_USER atanır")
    void shouldAssignDefaultRole() {
        SignUpUser input = buildInput("emre", "emre@example.com");

        SignUpUser result = handler.handle(input);

        assertThat(result.getRole()).isEqualTo(Set.of(Role.ROLE_CUSTOM_USER));
    }

    @Test
    @DisplayName("Kaydedilen kullanıcı persistence port'a iletilir")
    void shouldPersistUser() {
        SignUpUser input = buildInput("emre", "emre@example.com");

        handler.handle(input);

        assertThat(fakePort.getSavedUser()).isNotNull();
        assertThat(fakePort.getSavedUser().getUsername()).isEqualTo("emre");
    }

    // ── Username conflict ────────────────────────────────────────────────────

    @Test
    @DisplayName("Mevcut username ile kayıt → UserAlreadyExistsException fırlatılır")
    void shouldThrowWhenUsernameAlreadyExists() {
        fakePort = new FakeSignUpUserPersistenceAuthPort(Set.of("emre"), new HashSet<>());
        handler  = new SignUpUserHandler(fakePort);

        SignUpUser input = buildInput("emre", "new@example.com");

        assertThatThrownBy(() -> handler.handle(input))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("emre");
    }

    @Test
    @DisplayName("Mevcut email ile kayıt → UserAlreadyExistsException fırlatılır")
    void shouldThrowWhenEmailAlreadyExists() {
        fakePort = new FakeSignUpUserPersistenceAuthPort(new HashSet<>(), Set.of("emre@example.com"));
        handler  = new SignUpUserHandler(fakePort);

        SignUpUser input = buildInput("newuser", "emre@example.com");

        assertThatThrownBy(() -> handler.handle(input))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("emre@example.com");
    }

    @Test
    @DisplayName("Her kayıtta farklı UUID üretilir")
    void shouldGenerateUniqueUserIdPerRegistration() {
        SignUpUser first  = handler.handle(buildInput("user1", "u1@example.com"));

        fakePort = new FakeSignUpUserPersistenceAuthPort(new HashSet<>(), new HashSet<>());
        handler  = new SignUpUserHandler(fakePort);
        SignUpUser second = handler.handle(buildInput("user2", "u2@example.com"));

        assertThat(first.getUserId()).isNotEqualTo(second.getUserId());
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private SignUpUser buildInput(String username, String email) {
        SignUpUser u = new SignUpUser();
        u.setName("Test User");
        u.setUsername(username);
        u.setPassword("secret");
        u.setEmail(email);
        u.setBirthday(
                LocalDate.parse("1990-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")
        ).atStartOfDay());
        return u;
    }
}
