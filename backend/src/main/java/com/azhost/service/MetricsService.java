package com.azhost.service;

import com.azhost.build.BuildManager;
import com.azhost.deployment.DeploymentManager;
import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    // Custom build counters
    private final Counter buildSuccessCounter;
    private final Counter buildFailedCounter;
    private final Counter buildCancelledCounter;
    private final Counter buildTimeoutCounter;

    // Custom deployment counters
    private final Counter deploymentSuccessCounter;
    private final Counter deploymentFailedCounter;

    // Docker failure counters
    private final Counter dockerCreateFailuresCounter;
    private final Counter dockerStartFailuresCounter;
    private final Counter dockerCleanupFailuresCounter;

    // GitHub webhook counters
    private final Counter webhooksReceivedCounter;
    private final Counter webhooksRejectedCounter;
    private final Counter webhooksDuplicateCounter;
    private final Counter webhooksFailedCounter;

    // Timers
    private final Timer buildDurationTimer;
    private final Timer deploymentDurationTimer;

    public MetricsService(MeterRegistry registry, 
                          @org.springframework.context.annotation.Lazy BuildManager buildManager, 
                          @org.springframework.context.annotation.Lazy DeploymentManager deploymentManager) {
        this.registry = registry;

        // Active and Queued builds gauge mapping directly to the BuildManager collections
        Gauge.builder("azhost_active_builds", buildManager, BuildManager::getActiveBuildsCount)
                .description("Number of builds currently executing active compilation/isolation tasks")
                .register(registry);

        Gauge.builder("azhost_queued_builds", buildManager, BuildManager::getQueuedBuildsCount)
                .description("Number of builds pending execution slots in queue")
                .register(registry);

        // Active deployments gauge mapping directly to the DeploymentManager collection
        Gauge.builder("azhost_active_deployments", deploymentManager, DeploymentManager::getActiveDeploymentsCount)
                .description("Number of deployments currently being extracted and published")
                .register(registry);

        // Build total counter tagged by status
        this.buildSuccessCounter = Counter.builder("azhost_builds_total")
                .tag("status", "success")
                .description("Total number of builds executed")
                .register(registry);
        this.buildFailedCounter = Counter.builder("azhost_builds_total")
                .tag("status", "failed")
                .description("Total number of builds executed")
                .register(registry);
        this.buildCancelledCounter = Counter.builder("azhost_builds_total")
                .tag("status", "cancelled")
                .description("Total number of builds executed")
                .register(registry);
        this.buildTimeoutCounter = Counter.builder("azhost_builds_total")
                .tag("status", "timeout")
                .description("Total number of builds executed")
                .register(registry);

        // Deployment total counter tagged by status
        this.deploymentSuccessCounter = Counter.builder("azhost_deployments_total")
                .tag("status", "success")
                .description("Total number of deployments processed")
                .register(registry);
        this.deploymentFailedCounter = Counter.builder("azhost_deployments_total")
                .tag("status", "failed")
                .description("Total number of deployments processed")
                .register(registry);

        // Docker counters
        this.dockerCreateFailuresCounter = Counter.builder("azhost_docker_create_failures")
                .description("Total failures creating builds docker containers")
                .register(registry);
        this.dockerStartFailuresCounter = Counter.builder("azhost_docker_start_failures")
                .description("Total failures starting builds docker containers")
                .register(registry);
        this.dockerCleanupFailuresCounter = Counter.builder("azhost_docker_cleanup_failures")
                .description("Total failures cleaning up builds docker containers")
                .register(registry);

        // GitHub webhook counters
        this.webhooksReceivedCounter = Counter.builder("azhost_webhooks_received")
                .description("Total GitHub webhooks received")
                .register(registry);
        this.webhooksRejectedCounter = Counter.builder("azhost_webhooks_rejected")
                .description("Total GitHub webhooks rejected due to authentication/signatures")
                .register(registry);
        this.webhooksDuplicateCounter = Counter.builder("azhost_webhooks_duplicate")
                .description("Total duplicate GitHub webhooks detected")
                .register(registry);
        this.webhooksFailedCounter = Counter.builder("azhost_webhooks_failed")
                .description("Total GitHub webhooks processing failures")
                .register(registry);

        // Duration Timers
        this.buildDurationTimer = Timer.builder("azhost_build_duration")
                .description("Time taken to complete a workspace build task")
                .register(registry);
        this.deploymentDurationTimer = Timer.builder("azhost_deployment_duration")
                .description("Time taken to publish a deployment")
                .register(registry);
    }

    public void incrementBuildSuccess() {
        buildSuccessCounter.increment();
    }

    public void incrementBuildFailed() {
        buildFailedCounter.increment();
    }

    public void incrementBuildCancelled() {
        buildCancelledCounter.increment();
    }

    public void incrementBuildTimeout() {
        buildTimeoutCounter.increment();
    }

    public void incrementDeploymentSuccess() {
        deploymentSuccessCounter.increment();
    }

    public void incrementDeploymentFailed() {
        deploymentFailedCounter.increment();
    }

    public void incrementDockerCreateFailures() {
        dockerCreateFailuresCounter.increment();
    }

    public void incrementDockerStartFailures() {
        dockerStartFailuresCounter.increment();
    }

    public void incrementDockerCleanupFailures() {
        dockerCleanupFailuresCounter.increment();
    }

    public void incrementWebhooksReceived() {
        webhooksReceivedCounter.increment();
    }

    public void incrementWebhooksRejected() {
        webhooksRejectedCounter.increment();
    }

    public void incrementWebhooksDuplicate() {
        webhooksDuplicateCounter.increment();
    }

    public void incrementWebhooksFailed() {
        webhooksFailedCounter.increment();
    }

    public void recordBuildDuration(long durationMs) {
        buildDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordDeploymentDuration(long durationMs) {
        deploymentDurationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
