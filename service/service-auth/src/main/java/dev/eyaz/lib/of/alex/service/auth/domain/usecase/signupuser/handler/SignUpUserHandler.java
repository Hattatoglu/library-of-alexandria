package dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port.SignUpUserPersistenceAuthPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SignUpUserHandler implements UseCaseHandler<SignUpUser> {

    private final SignUpUserPersistenceAuthPort signUpUserPersistenceAuthPort;

    public SignUpUserHandler(SignUpUserPersistenceAuthPort signUpUserPersistenceAuthPort) {
        this.signUpUserPersistenceAuthPort = signUpUserPersistenceAuthPort;
    }

    @Override
    public SignUpUser handle(SignUpUser usecase) {
        SignUpUser checkedUser = checkUsernameAndMailExist(usecase);
        SignUpUser initializedUser = initiateUser(checkedUser);
        return saveUserDetails(initializedUser);
    }

    private SignUpUser checkUsernameAndMailExist(SignUpUser usecase) {
        return signUpUserPersistenceAuthPort.checkUsernameAndMail(usecase);
    }

    private SignUpUser initiateUser(SignUpUser usecase) {
        usecase.setUserId(UUID.randomUUID());
        usecase.setRole(Set.of(Role.ROLE_CUSTOM_USER));
        usecase.setAccountNonExpired(true);
        usecase.setAccountNonLocked(true);
        usecase.setCredentialsNonExpired(true);
        usecase.setEnabled(true);
        return usecase;
    }

    private SignUpUser saveUserDetails(SignUpUser usecase) {
        return signUpUserPersistenceAuthPort.saveUser(usecase);
    }
}
