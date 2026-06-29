package dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.port;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUser;

public interface SignUpUserPersistenceAuthPort {
    SignUpUser checkUsernameAndMail(SignUpUser usecase);
    SignUpUser saveUser(SignUpUser usecase);
}
