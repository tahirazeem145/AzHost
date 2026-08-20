package com.azhost.controller;

import com.azhost.config.DevUserInitializer;
import com.azhost.entity.EnvironmentVariableEntity;
import com.azhost.service.AuditLogService;
import com.azhost.service.EnvironmentVariableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/env")
public class EnvironmentVariableController {

    private final EnvironmentVariableService service;
    private final AuditLogService auditLogService;

    public EnvironmentVariableController(EnvironmentVariableService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<EnvVarResponseDto>> getVariables(@PathVariable UUID projectId, Principal principal) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        List<EnvironmentVariableEntity> vars = service.getVariablesForProject(projectId, email);
        List<EnvVarResponseDto> response = vars.stream()
                .map(v -> new EnvVarResponseDto(v, service.decryptValue(v)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EnvVarResponseDto> createVariable(
            @PathVariable UUID projectId,
            @RequestBody Map<String, Object> body,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        String name = (String) body.get("name");
        String value = (String) body.get("value");
        boolean secret = body.get("secret") != null && (Boolean) body.get("secret");
        String environment = (String) body.get("environment");

        EnvironmentVariableEntity entity = service.createVariable(
                projectId, name, value, secret, environment, email
        );

        auditLogService.log(
                entity.getProject().getUser(),
                entity.getProject(),
                "ENV_CREATED",
                "EnvironmentVariable",
                entity.getId().toString(),
                "SUCCESS",
                "Created env variable: " + name
        );

        return new ResponseEntity<>(new EnvVarResponseDto(entity, service.decryptValue(entity)), HttpStatus.CREATED);
    }

    @PutMapping("/{varId}")
    public ResponseEntity<EnvVarResponseDto> updateVariable(
            @PathVariable UUID projectId,
            @PathVariable UUID varId,
            @RequestBody Map<String, Object> body,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        String value = (String) body.get("value");
        boolean secret = body.get("secret") != null && (Boolean) body.get("secret");
        String environment = (String) body.get("environment");

        EnvironmentVariableEntity entity = service.updateVariable(
                projectId, varId, value, secret, environment, email
        );

        auditLogService.log(
                entity.getProject().getUser(),
                entity.getProject(),
                "ENV_UPDATED",
                "EnvironmentVariable",
                entity.getId().toString(),
                "SUCCESS",
                "Updated env variable: " + entity.getName()
        );

        return ResponseEntity.ok(new EnvVarResponseDto(entity, service.decryptValue(entity)));
    }

    @DeleteMapping("/{varId}")
    public ResponseEntity<Void> deleteVariable(
            @PathVariable UUID projectId,
            @PathVariable UUID varId,
            Principal principal
    ) {
        String email = principal != null ? principal.getName() : DevUserInitializer.DEV_USER_EMAIL;
        List<EnvironmentVariableEntity> vars = service.getVariablesForProject(projectId, email);
        EnvironmentVariableEntity target = vars.stream()
                .filter(v -> v.getId().equals(varId))
                .findFirst()
                .orElse(null);

        service.deleteVariable(projectId, varId, email);

        if (target != null) {
            auditLogService.log(
                    target.getProject().getUser(),
                    target.getProject(),
                    "ENV_DELETED",
                    "EnvironmentVariable",
                    varId.toString(),
                    "SUCCESS",
                    "Deleted env variable: " + target.getName()
            );
        }

        return ResponseEntity.noContent().build();
    }

    public static class EnvVarResponseDto {
        private UUID id;
        private String name;
        private boolean secret;
        private String environment;
        private String value;

        public EnvVarResponseDto(EnvironmentVariableEntity entity, String decryptedValue) {
            this.id = entity.getId();
            this.name = entity.getName();
            this.secret = entity.isSecret();
            this.environment = entity.getEnvironment();
            this.value = entity.isSecret() ? "••••••••" : decryptedValue;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public boolean isSecret() { return secret; }
        public String getEnvironment() { return environment; }
        public String getValue() { return value; }
    }
}
