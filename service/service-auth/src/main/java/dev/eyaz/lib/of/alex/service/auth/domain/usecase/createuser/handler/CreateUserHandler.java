package dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port.CreateUserPersistencePort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port.CreateUserSecurityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateUserHandler implements UseCaseHandler<CreateUser> {

    private final CreateUserPersistencePort createUserPersistencePort;
    private final CreateUserSecurityPort createUserSecurityPort;

    public CreateUserHandler(CreateUserPersistencePort createUserPersistencePort,
                             CreateUserSecurityPort createUserSecurityPort) {
        this.createUserPersistencePort = createUserPersistencePort;
        this.createUserSecurityPort = createUserSecurityPort;
    }

    @Override
    public CreateUser handle(CreateUser usecase) {
        CreateUser checkedUser = checkUsernameAndMailExist(usecase);
        CreateUser initializedUser = initiateUser(checkedUser);
        CreateUser userTokens = generateTokens(initializedUser);
        return saveUserDetails(userTokens);
    }

    private CreateUser checkUsernameAndMailExist(CreateUser usecase) {
        return createUserPersistencePort.checkUsernameAndMail(usecase);
    }

    private CreateUser initiateUser(CreateUser usecase) {
        usecase.setUserId(UUID.randomUUID());
        usecase.setRole(UserRole.ROLE_CUSTOM_USER);
        return usecase;
    }

    private CreateUser generateTokens(CreateUser usecase) {
        return createUserSecurityPort.generateAccessAndRefreshTokens(usecase);
    }

    private CreateUser saveUserDetails(CreateUser usecase) {
        return createUserPersistencePort.saveUser(usecase);
    }
}
