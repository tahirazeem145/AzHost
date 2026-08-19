package com.azhost.github;

import com.azhost.build.BuildStatus;
import com.azhost.dto.BuildResponseDto;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.dto.DeploymentResponseDto;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.repository.ProjectRepository;
import com.azhost.service.BuildService;
import com.azhost.service.DeploymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubBuildDeployServiceTest {

    private BuildService buildService;
    private DeploymentService deploymentService;
    private GitHubRepositoryService repositoryService;
    private ProjectRepository projectRepository;
    private GitHubBuildDeployService buildDeployService;

    private User testUser;
    private Project project;
    private static final String USER_EMAIL = "developer@azhost.dev";

    @BeforeEach
    void setUp() {
        buildService = mock(BuildService.class);
        deploymentService = mock(DeploymentService.class);
        repositoryService = mock(GitHubRepositoryService.class);
        projectRepository = mock(ProjectRepository.class);

        buildDeployService = new GitHubBuildDeployService(
                buildService, deploymentService, repositoryService, projectRepository
        );

        testUser = new User(USER_EMAIL, "hash", "Dev");
        testUser.setId(UUID.randomUUID());

        project = new Project(testUser, "TripNest", "tripnest", "Desc",
                ProjectFramework.REACT, ProjectSourceType.GITHUB, "https://github.com/user/TripNest", "main");
        project.setId(UUID.randomUUID());
        project.setGithubRepositoryId(12345L);
        project.setGithubBranch("main");
        project.setGithubCommitSha("abc123def456");

        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldThrowWhenProjectSourceTypeIsNotGitHub() {
        project.setSourceType(ProjectSourceType.LOCAL);

        assertThrows(IllegalArgumentException.class, () ->
                buildDeployService.triggerBuildAndDeploy(project, null, "test", USER_EMAIL));

        verifyNoInteractions(buildService, deploymentService);
    }

    @Test
    void shouldThrowWhenNoRepositoryLinked() {
        project.setGithubRepositoryId(null);

        assertThrows(IllegalArgumentException.class, () ->
                buildDeployService.triggerBuildAndDeploy(project, null, "test", USER_EMAIL));

        verifyNoInteractions(buildService, deploymentService);
    }

    @Test
    void shouldUseOverrideCommitShaWhenProvided() throws Exception {
        String overrideSha = "override-commit-sha";

        BuildResponseDto buildDto = new BuildResponseDto();
        buildDto.setId(UUID.randomUUID());
        buildDto.setStatus(BuildStatus.SUCCESS);
        when(buildService.startBuild(project.getId(), USER_EMAIL)).thenReturn(buildDto);
        when(buildService.getBuildById(eq(project.getId()), eq(buildDto.getId()), eq(USER_EMAIL)))
                .thenReturn(buildDto);

        DeploymentResponseDto deploymentDto = new DeploymentResponseDto();
        deploymentDto.setId(UUID.randomUUID());
        when(deploymentService.createDeployment(eq(project.getId()), any(CreateDeploymentRequest.class), eq(USER_EMAIL)))
                .thenReturn(deploymentDto);

        buildDeployService.triggerBuildAndDeploy(project, overrideSha, "test", USER_EMAIL);

        // Should have persisted the override SHA
        verify(projectRepository).save(argThat(p -> overrideSha.equals(p.getGithubCommitSha())));
        verify(buildService).startBuild(project.getId(), USER_EMAIL);
        verify(deploymentService).createDeployment(eq(project.getId()), any(), eq(USER_EMAIL));
    }

    @Test
    void shouldFallBackToExistingCommitShaWhenBranchResolutionFails() throws Exception {
        // GitHub API call fails
        when(repositoryService.resolveCommitSha(USER_EMAIL, 12345L, "main"))
                .thenThrow(new RuntimeException("GitHub API unavailable"));

        BuildResponseDto buildDto = new BuildResponseDto();
        buildDto.setId(UUID.randomUUID());
        buildDto.setStatus(BuildStatus.SUCCESS);
        when(buildService.startBuild(project.getId(), USER_EMAIL)).thenReturn(buildDto);
        when(buildService.getBuildById(any(), any(), any())).thenReturn(buildDto);

        DeploymentResponseDto deploymentDto = new DeploymentResponseDto();
        deploymentDto.setId(UUID.randomUUID());
        when(deploymentService.createDeployment(any(), any(), any())).thenReturn(deploymentDto);

        // Should succeed using fallback stored SHA "abc123def456"
        assertDoesNotThrow(() -> buildDeployService.triggerBuildAndDeploy(project, null, "test", USER_EMAIL));
        verify(buildService).startBuild(project.getId(), USER_EMAIL);
    }

    @Test
    void shouldNotDeployWhenBuildFails() throws Exception {
        BuildResponseDto buildDto = new BuildResponseDto();
        buildDto.setId(UUID.randomUUID());
        buildDto.setStatus(BuildStatus.FAILED);

        when(repositoryService.resolveCommitSha(USER_EMAIL, 12345L, "main")).thenReturn("abc123def456");
        when(buildService.startBuild(project.getId(), USER_EMAIL)).thenReturn(buildDto);
        when(buildService.getBuildById(any(), any(), any())).thenReturn(buildDto);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                buildDeployService.triggerBuildAndDeploy(project, "abc123def456", "test", USER_EMAIL));

        assertTrue(ex.getMessage().contains("FAILED") || ex.getMessage().contains("failed"),
                "Exception should indicate build failure");
        verifyNoInteractions(deploymentService);
    }

    @Test
    void shouldThrowWhenNoCommitShaAvailableAtAll() {
        project.setGithubCommitSha(null);
        project.setGithubBranch(null);

        when(repositoryService.resolveCommitSha(any(), any(), any()))
                .thenThrow(new RuntimeException("No branch"));

        assertThrows(Exception.class, () ->
                buildDeployService.triggerBuildAndDeploy(project, null, "test", USER_EMAIL));

        verifyNoInteractions(buildService, deploymentService);
    }
}
