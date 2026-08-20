package com.azhost.repository;

import com.azhost.entity.ProjectMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, UUID> {
    Optional<ProjectMemberEntity> findByProjectIdAndUserEmail(UUID projectId, String userEmail);
    List<ProjectMemberEntity> findByProjectId(UUID projectId);
    boolean existsByProjectIdAndUserEmail(UUID projectId, String userEmail);
}
