package com.azhost.repository;

import com.azhost.entity.ProjectBuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectBuildRepository extends JpaRepository<ProjectBuildEntity, UUID> {

    List<ProjectBuildEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    org.springframework.data.domain.Page<ProjectBuildEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId, org.springframework.data.domain.Pageable pageable);

    Optional<ProjectBuildEntity> findByIdAndProjectId(UUID id, UUID projectId);

    @Query("SELECT b FROM ProjectBuildEntity b WHERE b.status = com.azhost.build.BuildStatus.QUEUED ORDER BY b.createdAt ASC")
    List<ProjectBuildEntity> findQueuedBuilds();

    @Query("SELECT COUNT(b) FROM ProjectBuildEntity b WHERE b.project.id = :projectId AND b.status IN (com.azhost.build.BuildStatus.PREPARING, com.azhost.build.BuildStatus.INSTALLING, com.azhost.build.BuildStatus.BUILDING)")
    long countActiveBuildsForProject(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(b) FROM ProjectBuildEntity b WHERE b.project.user.id = :userId AND b.status IN (com.azhost.build.BuildStatus.PREPARING, com.azhost.build.BuildStatus.INSTALLING, com.azhost.build.BuildStatus.BUILDING)")
    long countActiveBuildsForUser(@Param("userId") UUID userId);

    @Query("SELECT b FROM ProjectBuildEntity b WHERE b.status IN (com.azhost.build.BuildStatus.PREPARING, com.azhost.build.BuildStatus.INSTALLING, com.azhost.build.BuildStatus.BUILDING) AND b.heartbeatAt < :threshold")
    List<ProjectBuildEntity> findStaleClaimedBuilds(@Param("threshold") ZonedDateTime threshold);

    @Modifying
    @Query("UPDATE ProjectBuildEntity b SET b.heartbeatAt = :now WHERE b.id = :id")
    int updateHeartbeat(@Param("id") UUID id, @Param("now") ZonedDateTime now);

    @Modifying
    @Query("UPDATE ProjectBuildEntity b SET b.status = com.azhost.build.BuildStatus.FAILED, b.errorMessage = :errorMessage, b.completedAt = :now WHERE b.claimedBy = :workerId AND b.status IN (com.azhost.build.BuildStatus.PREPARING, com.azhost.build.BuildStatus.INSTALLING, com.azhost.build.BuildStatus.BUILDING)")
    int failActiveBuildsForWorker(@Param("workerId") String workerId, @Param("errorMessage") String errorMessage, @Param("now") ZonedDateTime now);
}
