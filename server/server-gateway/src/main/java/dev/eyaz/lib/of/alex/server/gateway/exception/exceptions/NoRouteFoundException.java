package dev.eyaz.lib.of.alex.server.gateway.exception.exceptions;

import org.springframework.http.HttpStatus;

public class NoRouteFoundException extends GatewayException {

    public NoRouteFoundException(String path) {
        super("No route configured for path: " + path);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String errorCode() {
        return "route_not_found";
    }
}
