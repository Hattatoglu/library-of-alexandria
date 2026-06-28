package dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler.CreateUser;

public interface CreateUserPersistenceAuthPort {
    CreateUser checkUsernameAndMail(CreateUser usecase);
    CreateUser saveUser(CreateUser usecase);
}
