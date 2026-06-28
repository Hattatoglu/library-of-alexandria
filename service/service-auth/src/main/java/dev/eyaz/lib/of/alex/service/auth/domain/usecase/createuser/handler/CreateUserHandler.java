package dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port.CreateUserPersistencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateUserHandler implements UseCaseHandler<CreateUser> {

    private final CreateUserPersistencePort createUserPersistencePort;

    public CreateUserHandler(CreateUserPersistencePort createUserPersistencePort) {
        this.createUserPersistencePort = createUserPersistencePort;
    }

    @Override
    public CreateUser handle(CreateUser usecase) {
        CreateUser checkedUser = checkUsernameAndMailExist(usecase);
        CreateUser initializedUser = initiateUser(checkedUser);
        return saveUserDetails(initializedUser);
    }

    private CreateUser checkUsernameAndMailExist(CreateUser usecase) {
        return createUserPersistencePort.checkUsernameAndMail(usecase);
    }

    private CreateUser initiateUser(CreateUser usecase) {
        usecase.setUserId(UUID.randomUUID());
        usecase.setRole(Set.of(UserRole.ROLE_CUSTOM_USER));
        usecase.setAccountNonExpired(true);
        usecase.setAccountNonLocked(true);
        usecase.setCredentialsNonExpired(true);
        usecase.setEnabled(true);
        return usecase;
    }

    private CreateUser saveUserDetails(CreateUser usecase) {
        return createUserPersistencePort.saveUser(usecase);
    }
}
