package dev.eyaz.lib.of.alex.artifactory.lib.outbox.repository;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntity, UUID> {

    Optional<List<OutboxEntity>> findByOutboxStatus(String outboxStatus);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEntity entity SET entity.outboxStatus = :outboxStatus WHERE entity.id =:id")
    int updateOutboxStatusById(UUID id, String outboxStatus);
}
