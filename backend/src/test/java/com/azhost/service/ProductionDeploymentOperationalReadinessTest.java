package com.azhost.service;

import com.azhost.build.BuildManager;
import com.azhost.build.BuildStatus;
import com.azhost.build.BuildTask;
import com.azhost.config.ProductionConfigValidator;
import com.azhost.controller.HealthController;
import com.azhost.deployment.DeploymentManager;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.exception.GlobalExceptionHandler;
import com.azhost.security.CorrelationIdFilter;
import com.azhost.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class ProductionDeploymentOperationalReadinessTest {

    private MeterRegistry meterRegistry;
    private BuildManager buildManager;
    private DeploymentManager deploymentManager;
    private MetricsService metricsService;

    @BeforeEach
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        buildManager = mock(BuildManager.class);
        deploymentManager = mock(DeploymentManager.class);
        metricsService = new MetricsService(meterRegistry, buildManager, deploymentManager);
    }

    @Test
    public void testCorrelationIdFilter() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Case 1: Header missing
        filter.doFilter(request, response, chain);
        String generatedId = response.getHeader("X-Request-ID");
        assertThat(generatedId).isNotNull();
        assertThat(generatedId).hasSize(36); // UUID length
        assertThat(MDC.get("requestId")).isNull(); // MDC cleared

        // Case 2: Valid header supplied
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        String sampleId = UUID.randomUUID().toString();
        request.addHeader("X-Request-ID", sampleId);

        // We mock chain to verify MDC during execution
        doAnswer(invocation -> {
            assertThat(MDC.get("requestId")).isEqualTo(sampleId);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);
        assertThat(response.getHeader("X-Request-ID")).isEqualTo(sampleId);
    }

    @Test
    public void testProductionExceptionSanitization() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        GlobalExceptionHandler handler = new GlobalExceptionHandler(env);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        MDC.put("requestId", "corr-1234");
        try {
            ResponseEntity<Object> response = handler.handleGlobalException(new RuntimeException("SQL syntax error in table X"), request);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.get("code")).isEqualTo("INTERNAL_ERROR");
            assertThat(body.get("message")).isEqualTo("An unexpected error occurred.");
            assertThat(body.get("requestId")).isEqualTo("corr-1234");
            assertThat(body.toString()).doesNotContain("SQL syntax error");
        } finally {
            MDC.clear();
        }
    }

    @Test
    public void testProductionConfigValidatorFailsFast() {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(env.getProperty(anyString())).thenReturn(null); // All properties missing

        ProductionConfigValidator validator = new ProductionConfigValidator(env);
        assertThatThrownBy(() -> validator.onApplicationEvent(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Mandatory production configuration missing");
    }

    @Test
    public void testMetricsCounterIncrements() {
        metricsService.incrementBuildSuccess();
        metricsService.incrementBuildFailed();
        metricsService.incrementBuildCancelled();
        metricsService.incrementBuildTimeout();

        metricsService.incrementDeploymentSuccess();
        metricsService.incrementDeploymentFailed();

        metricsService.incrementDockerCreateFailures();
        metricsService.incrementDockerStartFailures();
        metricsService.incrementDockerCleanupFailures();

        metricsService.incrementWebhooksReceived();
        metricsService.incrementWebhooksRejected();
        metricsService.incrementWebhooksDuplicate();
        metricsService.incrementWebhooksFailed();

        assertThat(meterRegistry.get("azhost_builds_total").tag("status", "success").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_builds_total").tag("status", "failed").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_builds_total").tag("status", "cancelled").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_builds_total").tag("status", "timeout").counter().count()).isEqualTo(1.0);

        assertThat(meterRegistry.get("azhost_deployments_total").tag("status", "success").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_deployments_total").tag("status", "failed").counter().count()).isEqualTo(1.0);

        assertThat(meterRegistry.get("azhost_docker_create_failures").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_docker_start_failures").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_docker_cleanup_failures").counter().count()).isEqualTo(1.0);

        assertThat(meterRegistry.get("azhost_webhooks_received").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_webhooks_rejected").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_webhooks_duplicate").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("azhost_webhooks_failed").counter().count()).isEqualTo(1.0);
    }
}
