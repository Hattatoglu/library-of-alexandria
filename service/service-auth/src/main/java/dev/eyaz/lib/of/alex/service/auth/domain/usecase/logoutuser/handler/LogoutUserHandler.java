package dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.logoutuser.port.LogoutUserPersistenceTokenPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogoutUserHandler implements UseCaseHandler<LogoutUser> {

    private final LogoutUserPersistenceTokenPort logoutUserPersistenceTokenPort;

    public LogoutUserHandler(LogoutUserPersistenceTokenPort logoutUserPersistenceTokenPort) {
        this.logoutUserPersistenceTokenPort = logoutUserPersistenceTokenPort;
    }

    @Override
    public LogoutUser handle(LogoutUser usecase) {
        return logoutUserPersistenceTokenPort.deleteRefreshTokenByToken(usecase);
    }
}
