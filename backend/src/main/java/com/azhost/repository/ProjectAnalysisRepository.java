package com.azhost.repository;

import com.azhost.entity.ProjectAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectAnalysisRepository extends JpaRepository<ProjectAnalysisEntity, UUID> {

    Optional<ProjectAnalysisEntity> findByProjectId(UUID projectId);
}
