package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.details;

import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import org.springframework.security.core.GrantedAuthority;

public class UserRoleGrantedAuthority implements GrantedAuthority {

    private final Role role;

    public UserRoleGrantedAuthority(Role role) {
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + role.name();
    }
}
