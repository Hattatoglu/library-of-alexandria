package dev.eyaz.lib.of.alex.service.catalog.core.enums;

public enum BookStatus {
    AVAILABLE("AVAILABLE"),
    MAINTENANCE("MAINTENANCE"),
    LOST("LOST"),
    REMOVED("REMOVED");

    private final String value;

    BookStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
