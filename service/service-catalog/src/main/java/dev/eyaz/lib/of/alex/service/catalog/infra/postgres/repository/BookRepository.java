package dev.eyaz.lib.of.alex.service.catalog.infra.postgres.repository;

import dev.eyaz.lib.of.alex.service.catalog.infra.postgres.model.BookEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<BookEntity, Long> {
}
