package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port.FindUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FindUserPersistenceAuthPortAdapter implements FindUserPersistenceAuthPort {

    private final UserAuthRepository userAuthRepository;

    public FindUserPersistenceAuthPortAdapter(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    public FindUser findUserByUsername(FindUser usecase) {

        Optional<UserAuthEntity> optional = userAuthRepository.findByUsername(usecase.getUsername());
        if(optional.isPresent()) {
            UserAuthEntity entity = optional.get();
            usecase.setName(entity.getName());
            usecase.setUserId(entity.getUserId());
            usecase.setRoles(entity.getAuthorities().stream()
                    .map(role -> UserRole.valueOf(role.name()))
                    .collect(Collectors.toSet()));
            return usecase;
        }
        throw new UserNotFoundException("User : " + usecase.getUsername() + " not found!");
    }
}
