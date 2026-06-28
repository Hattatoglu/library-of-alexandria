package dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;

public interface LoginUserRefreshTokenPersistencePort {

    LoginUser persistRefreshToken(LoginUser usecase);
}
