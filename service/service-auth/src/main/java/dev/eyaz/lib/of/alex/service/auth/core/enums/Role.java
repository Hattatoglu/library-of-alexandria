package dev.eyaz.lib.of.alex.service.auth.core.enums;

import java.util.Arrays;

public enum Role {
    ROLE_SUPER_USER("SUPER"),
    ROLE_ADMIN_USER("ADMIN"),
    ROLE_CUSTOM_USER("USER");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static Role fromValue(String value) {
        return Arrays.stream(values())
                .filter(role -> role.value.equals(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Unknown role: " + value));
    }
}
