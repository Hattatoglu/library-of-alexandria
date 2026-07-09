package dev.eyaz.lib.of.alex.service.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan
@EnableJpaRepositories
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {
        "dev.eyaz.lib.of.alex.service.catalog",
        "dev.eyaz.lib.of.alex.artifactory.lib.outbox",
        "dev.eyaz.lib.of.alex.artifactory.lib.kafka.model.service.catalog.event",
        "dev.eyaz.lib.of.alex.artifactory.lib.kafka.producer"
})
public class ServiceCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCatalogApplication.class, args);
    }
}
