package dev.eyaz.lib.of.alex.service.auth.unit.usecase.updateuser;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUserHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateUserHandler — Unit Tests")
class UpdateUserHandlerTest {

    private static class FakeUpdateUserPort implements UpdateUserPersistAuthPort {
        private final boolean userExists;
        private UpdateUser updated;

        FakeUpdateUserPort(boolean userExists) { this.userExists = userExists; }

        @Override
        public UpdateUser updateUserRole(UpdateUser usecase) {
            if (!userExists) throw new UserNotFoundException("User not found: " + usecase.getUsername());
            Set<Role> roles = new HashSet<>(Set.of(Role.ROLE_CUSTOM_USER));
            roles.add(usecase.getNewRole());
            usecase.setRole(roles);
            this.updated = usecase;
            return usecase;
        }

        UpdateUser getUpdated() { return updated; }
    }

    private FakeUpdateUserPort fakePort;
    private UpdateUserHandler handler;

    @BeforeEach
    void setUp() {
        fakePort = new FakeUpdateUserPort(true);
        handler  = new UpdateUserHandler(fakePort);
    }

    @Test
    @DisplayName("Kullanıcıya yeni rol başarıyla eklenir")
    void shouldAddNewRole() {
        UpdateUser input = buildInput("emre", Role.ROLE_ADMIN_USER);

        UpdateUser result = handler.handle(input);

        assertThat(result.getUserId()).isEqualTo(input.getUserId());
        assertThat(result.getRole()).contains(Role.ROLE_ADMIN_USER);
    }

    @Test
    @DisplayName("Role update sonrası mevcut roller korunur")
    void shouldPreserveExistingRoles() {
        UpdateUser input = buildInput("emre", Role.ROLE_ADMIN_USER);

        UpdateUser result = handler.handle(input);

        assertThat(result.getRole()).contains(Role.ROLE_CUSTOM_USER, Role.ROLE_ADMIN_USER);
    }

    @Test
    @DisplayName("Olmayan kullanıcıya rol atama → UserNotFoundException")
    void shouldThrowWhenUserNotFound() {
        handler = new UpdateUserHandler(new FakeUpdateUserPort(false));

        assertThatThrownBy(() -> handler.handle(buildInput("ghost", Role.ROLE_ADMIN_USER)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    @DisplayName("Port'a iletilen use case doğru username ve newRole içerir")
    void shouldDelegateCorrectDataToPort() {
        UpdateUser input = buildInput("emre", Role.ROLE_SUPER_USER);

        handler.handle(input);

        assertThat(fakePort.getUpdated().getUsername()).isEqualTo("emre");
        assertThat(fakePort.getUpdated().getNewRole()).isEqualTo(Role.ROLE_SUPER_USER);
    }

    private UpdateUser buildInput(String username, Role role) {
        UpdateUser u = new UpdateUser();
        u.setUsername(username);
        u.setUserId(UUID.randomUUID());
        u.setNewRole(role);
        return u;
    }
}
