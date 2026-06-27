package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserDetailService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;

    public UserDetailService(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserAuthEntity> option = userAuthRepository.findByUsername(username);
        if (option.isPresent()) {
            return option.get();
        }
        throw new UsernameNotFoundException("user with "+ username + "not found!");
    }
}
