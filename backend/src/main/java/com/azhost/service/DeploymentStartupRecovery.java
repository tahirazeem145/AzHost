package com.azhost.service;

import com.azhost.deployment.DeploymentStatus;
import com.azhost.entity.DeploymentEntity;
import com.azhost.repository.DeploymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
public class DeploymentStartupRecovery {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentStartupRecovery.class);

    private final DeploymentRepository deploymentRepository;

    public DeploymentStartupRecovery(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverStaleDeployments() {
        logger.info("[AZHOST DEPLOYMENT RECOVERY] Scanning for stale active deployments...");
        List<DeploymentEntity> deployments = deploymentRepository.findAll();
        for (DeploymentEntity dep : deployments) {
            if (!dep.getStatus().isTerminal()) {
                logger.warn("[AZHOST DEPLOYMENT RECOVERY] Recovering stale deployment {} with status {}", dep.getId(), dep.getStatus());
                dep.setStatus(DeploymentStatus.FAILED);
                dep.setErrorMessage("Deployment aborted due to application restart");
                dep.setFailedAt(ZonedDateTime.now());
                deploymentRepository.save(dep);
            }
        }
        logger.info("[AZHOST DEPLOYMENT RECOVERY] Deployment recovery check complete.");
    }
}
