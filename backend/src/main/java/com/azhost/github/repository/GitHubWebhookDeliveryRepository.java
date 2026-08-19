package com.azhost.github.repository;

import com.azhost.github.entity.GitHubWebhookDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubWebhookDeliveryRepository extends JpaRepository<GitHubWebhookDeliveryEntity, UUID> {

    /**
     * Check whether a specific delivery ID has already been recorded for a project.
     * Used for idempotency — GitHub can retry webhooks with the same X-GitHub-Delivery header.
     */
    boolean existsByProjectIdAndDeliveryId(UUID projectId, String deliveryId);

    Optional<GitHubWebhookDeliveryEntity> findByProjectIdAndDeliveryId(UUID projectId, String deliveryId);
}
