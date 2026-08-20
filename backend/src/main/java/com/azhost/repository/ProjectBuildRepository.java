package com.azhost.repository;

import com.azhost.entity.ProjectBuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectBuildRepository extends JpaRepository<ProjectBuildEntity, UUID> {

    List<ProjectBuildEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    org.springframework.data.domain.Page<ProjectBuildEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, org.springframework.data.domain.Pageable pageable);

    Optional<ProjectBuildEntity> findByIdAndProjectId(UUID id, UUID projectId);
}
