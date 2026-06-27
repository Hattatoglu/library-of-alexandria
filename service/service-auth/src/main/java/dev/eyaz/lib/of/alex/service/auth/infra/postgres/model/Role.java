package dev.eyaz.lib.of.alex.service.auth.infra.postgres.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ROLE_SUPER_USER("SUPER"),
    ROLE_ADMIN_USER("ADMIN"),
    ROLE_CUSTOM_USER("USER");

    private String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String getAuthority() {
        return name();
    }
}
