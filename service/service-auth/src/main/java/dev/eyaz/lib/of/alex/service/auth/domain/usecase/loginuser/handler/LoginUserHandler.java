package dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserRefreshTokenPersistenceTokenPort;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port.LoginUserSecurityPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LoginUserHandler implements UseCaseHandler<LoginUser> {

    private final LoginUserSecurityPort loginUserSecurityPort;
    private final LoginUserRefreshTokenPersistenceTokenPort loginUserRefreshTokenPersistenceTokenPort;

    public LoginUserHandler(LoginUserSecurityPort loginUserSecurityPort, LoginUserRefreshTokenPersistenceTokenPort loginUserRefreshTokenPersistenceTokenPort) {
        this.loginUserSecurityPort = loginUserSecurityPort;
        this.loginUserRefreshTokenPersistenceTokenPort = loginUserRefreshTokenPersistenceTokenPort;
    }

    @Override
    public LoginUser handle(LoginUser usecase) {
        LoginUser generateToken = generateAccessAndRefreshToken(usecase);
        return persistRefreshToken(generateToken);
    }

    private LoginUser generateAccessAndRefreshToken(LoginUser usecase) {
        return loginUserSecurityPort.generateAccessAndRefreshTokens(usecase);
    }

    private LoginUser persistRefreshToken(LoginUser usecase) {
        return loginUserRefreshTokenPersistenceTokenPort.persistRefreshToken(usecase);
    }
}
