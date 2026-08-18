package com.azhost.github.repository;

import com.azhost.github.entity.GitHubConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubConnectionRepository extends JpaRepository<GitHubConnectionEntity, UUID> {

    Optional<GitHubConnectionEntity> findByUserId(UUID userId);

    Optional<GitHubConnectionEntity> findByGithubUserId(Long githubUserId);

    void deleteByUserId(UUID userId);
}
