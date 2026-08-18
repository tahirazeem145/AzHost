package com.azhost.github.security;

import com.azhost.entity.User;

import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubAuthorizationException;
import com.azhost.github.exception.GitHubConnectionNotFoundException;

import com.azhost.github.repository.GitHubConnectionRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GitHubSecurityPolicy {

    private final GitHubConnectionRepository connectionRepository;

    public GitHubSecurityPolicy(GitHubConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    public GitHubConnectionEntity validateUserConnection(User user) {
        if (user == null) {
            throw new GitHubAuthorizationException("Authentication context is required");
        }
        return connectionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new GitHubConnectionNotFoundException("GitHub account is not connected for user: " + user.getEmail()));
    }

    public void validateProjectOwnership(UUID projectUserId, UUID currentUserId) {
        if (!projectUserId.equals(currentUserId)) {
            throw new GitHubAuthorizationException("Access denied: You do not own this project");
        }
    }
}
