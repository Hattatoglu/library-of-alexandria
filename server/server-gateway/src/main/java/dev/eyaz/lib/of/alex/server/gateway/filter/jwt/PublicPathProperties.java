package dev.eyaz.lib.of.alex.server.gateway.filter.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "gateway.security")
public record PublicPathProperties(
        List<PublicPath> paths
) {
    public boolean isPublic(String path) {
        return paths != null && paths.stream().anyMatch(
                publicPath -> path.equals(publicPath.publicPath()) || path.startsWith(publicPath.publicPath() + "/"));
    }
}

