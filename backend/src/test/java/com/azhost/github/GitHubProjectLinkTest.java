package com.azhost.github;

import com.azhost.dto.ProjectResponseDto;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.github.controller.GitHubController;
import com.azhost.github.dto.GitHubRepositoryDto;
import com.azhost.github.dto.LinkGitHubRequestDto;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.security.GitHubSecurityPolicy;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GitHubProjectLinkTest {

    private GitHubOAuthService oauthService;
    private GitHubRepositoryService repositoryService;
    private GitHubSecurityPolicy securityPolicy;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private GitHubController controller;

    private User testUser;
    private Project project;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        oauthService = mock(GitHubOAuthService.class);
        repositoryService = mock(GitHubRepositoryService.class);
        securityPolicy = mock(GitHubSecurityPolicy.class);
        projectRepository = mock(ProjectRepository.class);
        userRepository = mock(UserRepository.class);

        controller = new GitHubController(oauthService, repositoryService, securityPolicy, projectRepository, userRepository);

        testUser = new User("developer@azhost.dev", "hash", "Dev");
        testUser.setId(UUID.randomUUID());

        projectId = UUID.randomUUID();
        project = new Project(testUser, "TripNest", "tripnest", "Desc", ProjectFramework.REACT, ProjectSourceType.LOCAL, null, null);
        project.setId(projectId);

        when(userRepository.findByEmail("developer@azhost.dev")).thenReturn(Optional.of(testUser));
        when(projectRepository.findByIdAndUserId(projectId, testUser.getId())).thenReturn(Optional.of(project));
    }

    @Test
    void shouldLinkProjectToGitHubRepository() {
        LinkGitHubRequestDto request = new LinkGitHubRequestDto(12345L, "main");

        GitHubRepositoryDto repoDetails = new GitHubRepositoryDto(12345L, "TripNest", "testuser/TripNest", true, "main", "https://github.com/testuser/TripNest", "2026-08-18");
        when(repositoryService.getRepositoryDetails("developer@azhost.dev", 12345L)).thenReturn(repoDetails);
        when(repositoryService.resolveCommitSha("developer@azhost.dev", 12345L, "main")).thenReturn("abc123commitsha456789");
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ProjectResponseDto> response = controller.linkProjectGitHub(projectId, request);

        assertNotNull(response.getBody());
        assertEquals(ProjectSourceType.GITHUB, response.getBody().getSourceType());
        assertEquals(12345L, response.getBody().getGithubRepositoryId());
        assertEquals("testuser/TripNest", response.getBody().getGithubRepositoryName());
        assertEquals("main", response.getBody().getGithubBranch());
        assertEquals("abc123commitsha456789", response.getBody().getGithubCommitSha());
    }

    @Test
    void shouldUnlinkProjectFromGitHubRepository() {
        project.setSourceType(ProjectSourceType.GITHUB);
        project.setGithubRepositoryId(12345L);
        project.setGithubBranch("main");

        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<ProjectResponseDto> response = controller.unlinkProjectGitHub(projectId);

        assertNotNull(response.getBody());
        assertEquals(ProjectSourceType.LOCAL, response.getBody().getSourceType());
        assertNull(response.getBody().getGithubRepositoryId());
        assertNull(response.getBody().getGithubBranch());
    }
}
