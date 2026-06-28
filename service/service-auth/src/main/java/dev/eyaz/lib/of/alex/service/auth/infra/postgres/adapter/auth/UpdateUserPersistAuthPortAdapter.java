package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.core.exception.UserNotFoundException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.Role;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
            Set<Role> roles = entity.getAuthorities();
            roles.add(Role.valueOf(usecase.getNewRole().name()));
            entity.setAuthorities(roles);

            userAuthRepository.save(entity);

            usecase.setRole(roles.stream()
                    .map(role -> UserRole.valueOf(role.name()))
                    .collect(Collectors.toSet()));

            return usecase;
        }
        throw new UserNotFoundException("User : " + usecase.getUsername() + " not found!");
    }
}
