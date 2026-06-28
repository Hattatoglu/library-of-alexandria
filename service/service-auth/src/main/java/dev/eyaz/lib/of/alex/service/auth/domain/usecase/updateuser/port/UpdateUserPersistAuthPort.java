package dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;

public interface UpdateUserPersistAuthPort {
    UpdateUser updateUserRole(UpdateUser usecase);
}
