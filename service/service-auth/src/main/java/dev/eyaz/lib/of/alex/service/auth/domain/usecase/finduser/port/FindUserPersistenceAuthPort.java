package dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUser;

public interface FindUserPersistenceAuthPort {
    FindUser findUserByUsername(FindUser usecase);
}
