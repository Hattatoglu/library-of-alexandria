package dev.eyaz.lib.of.alex.service.catalog.core.enums;

public enum BookType {

    NOVEL("NOVEL"),
    HISTORY("HISTORY"),
    LITERATURE("LITERATURE"),
    SCIENTIFIC("SCIENTIFIC"),
    SCIENCE_FICTION("SCIENCE_FICTION"),
    POEM("POEM"),
    CULTURE("CULTURE");

    private final String value;

    BookType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
