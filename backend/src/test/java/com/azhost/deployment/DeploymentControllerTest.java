package com.azhost.deployment;

import com.azhost.config.DevUserInitializer;
import com.azhost.controller.DeploymentController;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.dto.DeploymentListResponseDto;
import com.azhost.dto.DeploymentResponseDto;
import com.azhost.exception.BuildNotSuccessfulException;
import com.azhost.exception.DeploymentAlreadyInProgressException;
import com.azhost.security.WebSecurityConfig;
import com.azhost.service.DeploymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeploymentController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class DeploymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeploymentService deploymentService;

    private UUID projectId;
    private UUID buildId;
    private UUID deploymentId;
    private DeploymentResponseDto sampleDto;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        buildId = UUID.randomUUID();
        deploymentId = UUID.randomUUID();

        sampleDto = new DeploymentResponseDto();
        sampleDto.setId(deploymentId);
        sampleDto.setProjectId(projectId);
        sampleDto.setBuildId(buildId);
        sampleDto.setArtifactId("artifact-123");
        sampleDto.setStatus(DeploymentStatus.QUEUED);
        sampleDto.setCreatedAt(ZonedDateTime.now());
    }

    @Test
    void createDeployment_WhenSuccessful_ShouldReturn202Accepted() throws Exception {
        given(deploymentService.createDeployment(eq(projectId), any(CreateDeploymentRequest.class), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(sampleDto);

        CreateDeploymentRequest req = new CreateDeploymentRequest(buildId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.artifactId").value("artifact-123"));
    }

    @Test
    void createDeployment_WhenBuildNotSuccessful_ShouldReturn409Conflict() throws Exception {
        given(deploymentService.createDeployment(eq(projectId), any(CreateDeploymentRequest.class), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willThrow(new BuildNotSuccessfulException("Only successful builds can be deployed."));

        CreateDeploymentRequest req = new CreateDeploymentRequest(buildId);

        mockMvc.perform(post("/api/projects/" + projectId + "/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("BUILD_NOT_SUCCESSFUL"));
    }

    @Test
    void getDeployments_ShouldReturnList() throws Exception {
        DeploymentListResponseDto listDto = new DeploymentListResponseDto(List.of(sampleDto));
        given(deploymentService.getDeploymentsForProject(eq(projectId), eq(DevUserInitializer.DEV_USER_EMAIL)))
                .willReturn(listDto);

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployments[0].id").value(deploymentId.toString()));
    }
}
