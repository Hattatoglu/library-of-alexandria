package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UpdateUserPersistAuthPortAdapter implements UpdateUserPersistAuthPort {

    private static final Logger log = LoggerFactory.getLogger(UpdateUserPersistAuthPortAdapter.class);

    private final UserAuthRepository userAuthRepository;
    private final AuthMetrics authMetrics;

    public UpdateUserPersistAuthPortAdapter(UserAuthRepository userAuthRepository, AuthMetrics authMetrics) {
        this.userAuthRepository = userAuthRepository;
        this.authMetrics = authMetrics;
    }

    @Override
    public UpdateUser updateUserRole(UpdateUser usecase) {
        Optional<UserAuthEntity> optional = userAuthRepository.findByUsername(usecase.getUsername());
        if(optional.isPresent()) {
            UserAuthEntity entity = optional.get();
            entity.getRoles().add(usecase.getNewRole());

            userAuthRepository.save(entity);

            usecase.setRole(entity.getRoles());

            return usecase;
        }
        authMetrics.incrementRoleUpdateFailureUserNotFound();
        log.warn("action=role_update_user_not_found username={}", usecase.getUsername());
        throw new UserNotFoundException("User : " + usecase.getUsername() + " not found!");
    }
}
