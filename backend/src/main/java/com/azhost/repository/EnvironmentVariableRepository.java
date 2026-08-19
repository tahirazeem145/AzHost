package com.azhost.repository;

import com.azhost.entity.EnvironmentVariableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariableEntity, UUID> {
    List<EnvironmentVariableEntity> findByProjectId(UUID projectId);
    Optional<EnvironmentVariableEntity> findByProjectIdAndNameAndEnvironment(UUID projectId, String name, String environment);
    void deleteByProjectIdAndNameAndEnvironment(UUID projectId, String name, String environment);
}
