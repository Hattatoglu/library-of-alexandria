package dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.port.UpdateUserPersistAuthPort;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserHandler implements UseCaseHandler<UpdateUser> {

    private final UpdateUserPersistAuthPort updateUserPersistAuthPort;

    public UpdateUserHandler(UpdateUserPersistAuthPort updateUserPersistAuthPort) {
        this.updateUserPersistAuthPort = updateUserPersistAuthPort;
    }

    @Override
    public UpdateUser handle(UpdateUser usecase) {
        return updateUserPersistAuthPort.updateUserRole(usecase);
    }
}
