package dev.eyaz.lib.of.alex.service.catalog.core.exception;

import org.springframework.http.HttpStatus;

public abstract class ServiceCatalogException extends RuntimeException{

    protected ServiceCatalogException(String message) {
        super(message);
    }

    public abstract HttpStatus httpStatus();

    public abstract String errorCode();
}
