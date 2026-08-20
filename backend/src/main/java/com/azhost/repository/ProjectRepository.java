package com.azhost.repository;

import com.azhost.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    Optional<Project> findBySlugAndUserId(String slug, UUID userId);

    boolean existsByUserIdAndSlug(UUID userId, String slug);

    long countByUserId(UUID userId);

    @Query("SELECT p FROM Project p WHERE p.user.id = :userId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY p.createdAt DESC")
    List<Project> searchByUserIdAndQuery(@Param("userId") UUID userId, @Param("query") String query);

    /**
     * Find all projects linked to a specific GitHub repository ID.
     * Used by the webhook handler to identify the target project.
     */
    List<Project> findAllByGithubRepositoryId(Long githubRepositoryId);

    @Query(value = "SELECT * FROM projects WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Project> findAndLockById(@Param("id") UUID id);
}

