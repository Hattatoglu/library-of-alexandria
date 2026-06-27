package dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import org.springframework.stereotype.Service;

@Service
public class CreateUserHandler implements UseCaseHandler<CreateUser> {
    @Override
    public CreateUser handle(CreateUser usecase) {
        return null;
    }
}
