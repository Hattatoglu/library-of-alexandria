package dev.eyaz.lib.of.alex.service.auth.unit.usecase.refreshtoken;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidTokenException;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCaseSecurityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RefreshTokenUseCaseHandler — Unit Tests")
class RefreshTokenUseCaseHandlerTest {

    // ── Fakes ────────────────────────────────────────────────────────────────
    private static class FakeTokenPersistencePort implements RefreshTokenUseCasePersistenceTokenPort {
        private final boolean tokenExists;
        private String deletedToken;
        private String savedToken;
        private final UUID ownerUserId;

        FakeTokenPersistencePort(boolean tokenExists, UUID ownerUserId) {
            this.tokenExists = tokenExists;
            this.ownerUserId = ownerUserId;
        }

        @Override
        public RefreshTokenUseCase findRefreshTokenByToken(RefreshTokenUseCase useCase) {
            if (!tokenExists) throw new InvalidTokenException("Token Not Found: " + useCase.getRefreshToken());
            useCase.setUserId(ownerUserId);
            return useCase;
        }

        @Override
        public RefreshTokenUseCase saveNewRefreshTokenAndDeleteOldRefreshToken(RefreshTokenUseCase useCase) {
            this.deletedToken = useCase.getRefreshToken();
            this.savedToken   = useCase.getNewRefreshToken();
            return useCase;
        }

        String getDeletedToken() { return deletedToken; }
        String getSavedToken()   { return savedToken; }
    }

    private static class FakeAuthPort implements RefreshTokenUseCasePersistenceAuthPort {
        private final boolean userExists;

        FakeAuthPort(boolean userExists) { this.userExists = userExists; }

        @Override
        public RefreshTokenUseCase findUserDetails(RefreshTokenUseCase useCase) {
            if (!userExists) throw new UserNotFoundException("User not found: " + useCase.getUserId());
            useCase.setUsername("emre");
            useCase.setRoles(Set.of(Role.ROLE_CUSTOM_USER));
            return useCase;
        }
    }

    private static class FakeSecurityPort implements RefreshTokenUseCaseSecurityPort {
        @Override
        public RefreshTokenUseCase generateAccessAndRefreshToken(RefreshTokenUseCase usecase) {
            usecase.setNewAccessToken("new-access-token");
            usecase.setNewRefreshToken("new-refresh-token");
            usecase.setAccessTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
            usecase.setRefreshTokenExpiresAt(LocalDateTime.now().plusDays(7));
            return usecase;
        }
    }

    private final UUID userId = UUID.randomUUID();
    private FakeTokenPersistencePort fakeTokenPort;
    private RefreshTokenUseCaseHandler handler;

    @BeforeEach
    void setUp() {
        fakeTokenPort = new FakeTokenPersistencePort(true, userId);
        handler = new RefreshTokenUseCaseHandler(
                fakeTokenPort,
                new FakeAuthPort(true),
                new FakeSecurityPort()
        );
    }

    @Test
    @DisplayName("Geçerli token ile yeni access ve refresh token üretilir")
    void shouldRotateTokensSuccessfully() {
        RefreshTokenUseCase input = buildInput("old-refresh-token");

        RefreshTokenUseCase result = handler.handle(input);

        assertThat(result.getNewAccessToken()).isEqualTo("new-access-token");
        assertThat(result.getNewRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Token rotation: eski token silinir, yeni token kaydedilir")
    void shouldDeleteOldAndSaveNewToken() {
        RefreshTokenUseCase input = buildInput("old-refresh-token");

        handler.handle(input);

        assertThat(fakeTokenPort.getDeletedToken()).isEqualTo("old-refresh-token");
        assertThat(fakeTokenPort.getSavedToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("Bulunamayan token → InvalidTokenException")
    void shouldThrowWhenTokenNotFound() {
        handler = new RefreshTokenUseCaseHandler(
                new FakeTokenPersistencePort(false, userId),
                new FakeAuthPort(true),
                new FakeSecurityPort()
        );

        assertThatThrownBy(() -> handler.handle(buildInput("invalid-token")))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    @DisplayName("Token sahibi kullanıcı bulunamazsa → UserNotFoundException")
    void shouldThrowWhenUserNotFound() {
        handler = new RefreshTokenUseCaseHandler(
                new FakeTokenPersistencePort(true, userId),
                new FakeAuthPort(false),
                new FakeSecurityPort()
        );

        assertThatThrownBy(() -> handler.handle(buildInput("valid-token")))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Yeni token expiry tarihleri gelecekte olmalıdır")
    void newTokensShouldHaveFutureExpiry() {
        RefreshTokenUseCase result = handler.handle(buildInput("token"));

        assertThat(result.getAccessTokenExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(result.getRefreshTokenExpiresAt()).isAfter(LocalDateTime.now());
    }

    private RefreshTokenUseCase buildInput(String token) {
        RefreshTokenUseCase uc = new RefreshTokenUseCase();
        uc.setRefreshToken(token);
        return uc;
    }
}
