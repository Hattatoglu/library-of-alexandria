package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserAlreadyExistsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler.CreateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port.CreateUserPersistencePort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.Role;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;


@Component
public class CreateUserPersistencePortAdapter implements CreateUserPersistencePort {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserPersistencePortAdapter(UserAuthRepository userAuthRepository, PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public CreateUser checkUsernameAndMail(CreateUser usecase) {
        if (userAuthRepository.existsByUsername(usecase.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + usecase.getUsername());
        }
        if (userAuthRepository.existsByEmail(usecase.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + usecase.getEmail());
        }
        return usecase;
    }

    @Override
    public CreateUser saveUser(CreateUser usecase) {
        UserAuthEntity entity = new UserAuthEntity();
        entity.setUserId(usecase.getUserId());
        entity.setName(usecase.getName());
        entity.setUsername(usecase.getUsername());
        entity.setPassword(passwordEncoder.encode(usecase.getPassword()));
        entity.setEmail(usecase.getEmail());
        entity.setBirthdate(usecase.getBirthday());
        entity.setAuthorities(usecase.getRole()
                .stream()
                .map(role -> Role.valueOf(role.name()))
                .collect(Collectors.toSet()));
        entity.setAccountNonExpired(usecase.isAccountNonExpired());
        entity.setAccountNonLocked(usecase.isAccountNonLocked());
        entity.setCredentialsNonExpired(usecase.isCredentialsNonExpired());
        entity.setEnabled(usecase.isEnabled());

        try {
            userAuthRepository.save(entity);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }

        return usecase;
    }
}
