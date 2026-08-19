package com.azhost.service;

import com.azhost.entity.AuditLogEntity;
import com.azhost.entity.Project;
import com.azhost.entity.User;
import com.azhost.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, Project project, String action, String resourceType, String resourceId, String result, String metadata) {
        AuditLogEntity entity = new AuditLogEntity(user, project, action, resourceType, resourceId, result, metadata);
        repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogEntity> getAuditLogsForProject(UUID projectId, Pageable pageable) {
        return repository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
    }
}
