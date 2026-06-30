package dev.eyaz.lib.of.alex.service.auth.unit.usecase.finduser;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUserHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port.FindUserPersistenceAuthPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FindUserHandler — Unit Tests")
class FindUserHandlerTest {

    private static class FakeFindUserPort implements FindUserPersistenceAuthPort {
        private final boolean userExists;
        private final UUID fixedId = UUID.randomUUID();

        FakeFindUserPort(boolean userExists) { this.userExists = userExists; }

        @Override
        public FindUser findUserByUsername(FindUser usecase) {
            if (!userExists) throw new UserNotFoundException("User not found: " + usecase.getUsername());
            usecase.setName("Emre");
            usecase.setUserId(fixedId);
            usecase.setRoles(Set.of(Role.ROLE_CUSTOM_USER));
            return usecase;
        }

        UUID getFixedId() { return fixedId; }
    }

    private FakeFindUserPort fakePort;
    private FindUserHandler handler;

    @BeforeEach
    void setUp() {
        fakePort = new FakeFindUserPort(true);
        handler  = new FindUserHandler(fakePort);
    }

    @Test
    @DisplayName("Mevcut kullanıcı başarıyla bulunur ve alanları doldurulur")
    void shouldReturnUserDetails() {
        FindUser input = new FindUser();
        input.setUsername("emre");

        FindUser result = handler.handle(input);

        assertThat(result.getName()).isEqualTo("Emre");
        assertThat(result.getUserId()).isEqualTo(fakePort.getFixedId());
        assertThat(result.getRoles()).containsExactly(Role.ROLE_CUSTOM_USER);
    }

    @Test
    @DisplayName("Bulunamayan kullanıcı → UserNotFoundException")
    void shouldThrowWhenUserNotFound() {
        handler = new FindUserHandler(new FakeFindUserPort(false));

        FindUser input = new FindUser();
        input.setUsername("ghost");

        assertThatThrownBy(() -> handler.handle(input))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
