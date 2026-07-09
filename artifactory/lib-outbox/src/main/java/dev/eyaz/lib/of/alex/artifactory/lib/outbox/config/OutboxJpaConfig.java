package dev.eyaz.lib.of.alex.artifactory.lib.outbox.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = {"dev.eyaz.lib.of.alex.artifactory.lib.outbox"})
@EnableJpaRepositories(basePackages = {"dev.eyaz.lib.of.alex.artifactory.lib.outbox"})
public class OutboxJpaConfig {
}
