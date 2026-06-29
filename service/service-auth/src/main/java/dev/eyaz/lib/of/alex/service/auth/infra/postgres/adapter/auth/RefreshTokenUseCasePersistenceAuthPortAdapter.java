package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.handler.RefreshTokenUseCase;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.refreshtoken.port.RefreshTokenUseCasePersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RefreshTokenUseCasePersistenceAuthPortAdapter
        implements RefreshTokenUseCasePersistenceAuthPort {

    private final UserAuthRepository userAuthRepository;

    public RefreshTokenUseCasePersistenceAuthPortAdapter(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
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
        throw new UserNotFoundException("User : " + useCase.getUserId() + " not found!");
    }
}
