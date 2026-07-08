package dev.eyaz.lib.of.alex.service.catalog.infra.rest.exception;

import dev.eyaz.lib.of.alex.artifactory.lib.infra.exception.WebExceptionHandlerSupport;
import dev.eyaz.lib.of.alex.service.catalog.core.exception.InsufficientRoleException;
import dev.eyaz.lib.of.alex.service.catalog.infra.observability.CatalogMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ServiceCatalogExceptionHandler extends WebExceptionHandlerSupport {

    private static final Logger log = LoggerFactory.getLogger(ServiceCatalogExceptionHandler.class);
    private final CatalogMetrics catalogMetrics;

    public ServiceCatalogExceptionHandler(CatalogMetrics catalogMetrics) {
        this.catalogMetrics = catalogMetrics;
    }

    @ExceptionHandler(InsufficientRoleException.class)
    public ProblemDetail handleInsufficientRoleException(InsufficientRoleException ex) {
        catalogMetrics.incrementAddBookSuccess();
        log.warn("action=addbook_rejected type=insufficient_role detail = {}", ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "invalid-role", ex.getMessage());
    }

}
