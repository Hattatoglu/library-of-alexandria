package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenUseCasePersistenceAuthPortAdapter
        implements RefreshTokenUseCasePersistenceAuthPort {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCasePersistenceAuthPortAdapter.class);

    private final UserAuthRepository userAuthRepository;
    private final AuthMetrics authMetrics;

    public RefreshTokenUseCasePersistenceAuthPortAdapter(UserAuthRepository userAuthRepository, AuthMetrics authMetrics) {
        this.userAuthRepository = userAuthRepository;
        this.authMetrics = authMetrics;
    }

    @Override
    public RefreshTokenUseCase findUserDetails(RefreshTokenUseCase useCase) {
        Optional<UserAuthEntity> optional = userAuthRepository.findByUserId(useCase.getUserId());
        if(optional.isPresent()) {
            UserAuthEntity entity = optional.get();
            useCase.setUsername(entity.getUsername());
            useCase.setRoles(entity.getRoles());
            return useCase;
        }
        authMetrics.incrementTokenRefreshFailureUserNotFound();
        log.warn("action=token_refresh_user_not_found userId={}", useCase.getUserId());
        throw new UserNotFoundException("User : " + useCase.getUserId() + " not found!");
    }
}
