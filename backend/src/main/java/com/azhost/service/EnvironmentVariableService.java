package com.azhost.service;

import com.azhost.entity.EnvironmentVariableEntity;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectRole;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.repository.EnvironmentVariableRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class EnvironmentVariableService {

    private static final Pattern ENV_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final EnvironmentVariableRepository repository;
    private final GitHubTokenEncryptor encryptor;
    private final ProjectAuthorizationService projectAuthorizationService;

    public EnvironmentVariableService(
            EnvironmentVariableRepository repository,
            GitHubTokenEncryptor encryptor,
            ProjectAuthorizationService projectAuthorizationService
    ) {
        this.repository = repository;
        this.encryptor = encryptor;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentVariableEntity> getVariablesForProject(UUID projectId, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.VIEWER);
        return repository.findByProjectId(project.getId());
    }

    @Transactional
    public EnvironmentVariableEntity createVariable(
            UUID projectId,
            String name,
            String value,
            boolean isSecret,
            String environment,
            String userEmail
    ) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.OWNER);
        validateName(name);

        String envStr = (environment == null || environment.isBlank()) ? "production" : environment.trim().toLowerCase();
        
        repository.findByProjectIdAndNameAndEnvironment(project.getId(), name, envStr)
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Variable '" + name + "' already exists in environment: " + envStr);
                });

        String encrypted = encryptor.encrypt(value);
        EnvironmentVariableEntity entity = new EnvironmentVariableEntity(project, name, encrypted, isSecret, envStr);
        return repository.save(entity);
    }

    @Transactional
    public EnvironmentVariableEntity updateVariable(
            UUID projectId,
            UUID variableId,
            String value,
            boolean isSecret,
            String environment,
            String userEmail
    ) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.OWNER);
        EnvironmentVariableEntity entity = repository.findById(variableId)
                .orElseThrow(() -> new IllegalArgumentException("Environment variable not found with ID: " + variableId));

        if (!entity.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Environment variable does not belong to the specified project.");
        }

        if (value != null && !value.isBlank()) {
            entity.setEncryptedValue(encryptor.encrypt(value));
        }
        entity.setSecret(isSecret);
        if (environment != null && !environment.isBlank()) {
            entity.setEnvironment(environment.trim().toLowerCase());
        }

        return repository.save(entity);
    }

    @Transactional
    public void deleteVariable(UUID projectId, UUID variableId, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.OWNER);
        EnvironmentVariableEntity entity = repository.findById(variableId)
                .orElseThrow(() -> new IllegalArgumentException("Environment variable not found with ID: " + variableId));

        if (!entity.getProject().getId().equals(project.getId())) {
            throw new IllegalArgumentException("Environment variable does not belong to the specified project.");
        }

        repository.delete(entity);
    }

    public String decryptValue(EnvironmentVariableEntity entity) {
        return encryptor.decrypt(entity.getEncryptedValue());
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Environment variable name cannot be blank");
        }
        if (!ENV_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid environment variable name. It must start with a letter or underscore and contain only alphanumeric characters and underscores.");
        }
    }
}
