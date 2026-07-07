package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.auth;

import dev.eyaz.lib.of.alex.service.auth.core.exception.UserAlreadyExistsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port.SignUpUserPersistenceAuthPort;
import dev.eyaz.lib.of.alex.service.auth.infra.observability.AuthMetrics;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


@Component
public class SignUpUserPersistenceAuthPortAdapter implements SignUpUserPersistenceAuthPort {

    private static final Logger log = LoggerFactory.getLogger(SignUpUserPersistenceAuthPortAdapter.class);

    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMetrics authMetrics;

    public SignUpUserPersistenceAuthPortAdapter(UserAuthRepository userAuthRepository, PasswordEncoder passwordEncoder, AuthMetrics authMetrics) {
        this.userAuthRepository = userAuthRepository;
        this.passwordEncoder = passwordEncoder;
        this.authMetrics = authMetrics;
    }

    @Override
    public SignUpUser checkUsernameAndMail(SignUpUser usecase) {
        if (userAuthRepository.existsByUsername(usecase.getUsername())) {
            authMetrics.incrementSignUpFailureDuplicateUsername();
            log.warn("action=sign_up_duplicate_username username={}", usecase.getUsername());
            throw new UserAlreadyExistsException("Username already taken: " + usecase.getUsername());
        }
        if (userAuthRepository.existsByEmail(usecase.getEmail())) {
            authMetrics.incrementSignUpFailureDuplicateEmail();
            log.warn("action=sign_up_duplicate_email email={}", usecase.getEmail());
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

        userAuthRepository.save(entity);
        log.debug("action=sign_up_persisted username={} userId={}", usecase.getUsername(), usecase.getUserId());

        return usecase;
    }
}
