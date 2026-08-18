package com.azhost.repository;

import com.azhost.deployment.DeploymentStatus;
import com.azhost.entity.DeploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeploymentRepository extends JpaRepository<DeploymentEntity, UUID> {

    List<DeploymentEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<DeploymentEntity> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<DeploymentEntity> findFirstByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, DeploymentStatus status);
}
