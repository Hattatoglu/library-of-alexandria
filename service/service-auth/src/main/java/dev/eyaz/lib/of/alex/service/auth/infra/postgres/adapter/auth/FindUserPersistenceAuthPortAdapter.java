package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port.FindUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class FindUserPersistenceAuthPortAdapter implements FindUserPersistenceAuthPort {

    private static final Logger log = LoggerFactory.getLogger(FindUserPersistenceAuthPortAdapter.class);

    private final UserAuthRepository userAuthRepository;
    private final AuthMetrics authMetrics;

    public FindUserPersistenceAuthPortAdapter(UserAuthRepository userAuthRepository, AuthMetrics authMetrics) {
        this.userAuthRepository = userAuthRepository;
        this.authMetrics = authMetrics;
    }

    @Override
    public FindUser findUserByUsername(FindUser usecase) {

        Optional<UserAuthEntity> optional = userAuthRepository.findByUsername(usecase.getUsername());
        if(optional.isPresent()) {
            UserAuthEntity entity = optional.get();
            usecase.setName(entity.getName());
            usecase.setUserId(entity.getUserId());
            usecase.setRoles(entity.getRoles());
            return usecase;
        }
        authMetrics.incrementFindUserFailureNotFound();
        log.warn("action=find_user_not_found username={}", usecase.getUsername());
        throw new UserNotFoundException("User : " + usecase.getUsername() + " not found!");
    }
}
