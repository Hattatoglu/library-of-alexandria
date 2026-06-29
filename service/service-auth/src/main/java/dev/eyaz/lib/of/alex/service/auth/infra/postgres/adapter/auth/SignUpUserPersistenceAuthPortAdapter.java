package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserAlreadyExistsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port.SignUpUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class SignUpUserPersistenceAuthPortAdapter implements SignUpUserPersistenceAuthPort {

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;

    public SignUpUserPersistenceAuthPortAdapter(UserAuthRepository userAuthRepository, PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public SignUpUser checkUsernameAndMail(SignUpUser usecase) {
        if (userAuthRepository.existsByUsername(usecase.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + usecase.getUsername());
        }
        if (userAuthRepository.existsByEmail(usecase.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + usecase.getEmail());
        }
        return usecase;
    }

    @Override
    public SignUpUser saveUser(SignUpUser usecase) {
        UserAuthEntity entity = new UserAuthEntity();
        entity.setUserId(usecase.getUserId());
        entity.setName(usecase.getName());
        entity.setUsername(usecase.getUsername());
        entity.setPassword(passwordEncoder.encode(usecase.getPassword()));
        entity.setEmail(usecase.getEmail());
        entity.setBirthday(usecase.getBirthday());
        entity.setRoles(usecase.getRole());
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
