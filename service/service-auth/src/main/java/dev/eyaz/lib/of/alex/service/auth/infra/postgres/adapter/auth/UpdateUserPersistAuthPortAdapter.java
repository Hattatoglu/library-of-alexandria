package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UpdateUserPersistAuthPortAdapter implements UpdateUserPersistAuthPort {

    private final UserAuthRepository userAuthRepository;

    public UpdateUserPersistAuthPortAdapter(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
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
        throw new UserNotFoundException("User : " + usecase.getUsername() + " not found!");
    }
}
