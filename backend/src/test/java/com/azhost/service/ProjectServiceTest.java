package com.azhost.service;

import com.azhost.dto.CreateProjectRequest;
import com.azhost.dto.ProjectListResponseDto;
import com.azhost.dto.ProjectResponseDto;
import com.azhost.dto.UpdateProjectRequest;
import com.azhost.entity.*;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SlugService slugService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @InjectMocks
    private ProjectService projectService;

    private User sampleUser;
    private Project sampleProject;
    private final String email = "developer@azhost.dev";

    @BeforeEach
    void setUp() {
        sampleUser = new User(email, "pass", "Developer");
        sampleUser.setId(UUID.randomUUID());

        sampleProject = new Project(
                sampleUser,
                "TripNest",
                "tripnest",
                "Travel App",
                ProjectFramework.REACT,
                ProjectSourceType.GITHUB,
                "https://github.com/example/tripnest",
                "main"
        );
        sampleProject.setId(UUID.randomUUID());
    }

    @Test
    void createProject_ShouldSaveAndReturnResponse() {
        CreateProjectRequest request = new CreateProjectRequest("TripNest", "Travel App", ProjectFramework.REACT, ProjectSourceType.GITHUB, "https://github.com/example/tripnest", "main");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(sampleUser));
        given(slugService.generateUniqueSlug(sampleUser.getId(), "TripNest")).willReturn("tripnest");
        given(projectRepository.save(any(Project.class))).willReturn(sampleProject);

        ProjectResponseDto response = projectService.createProject(request, email);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("TripNest");
        assertThat(response.getSlug()).isEqualTo("tripnest");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void getProjects_ShouldReturnListAndTotalCount() {
        given(userRepository.findByEmail(email)).willReturn(Optional.of(sampleUser));
        given(projectRepository.findAllByUserIdOrderByCreatedAtDesc(sampleUser.getId())).willReturn(List.of(sampleProject));
        given(projectRepository.countByUserId(sampleUser.getId())).willReturn(1L);

        ProjectListResponseDto result = projectService.getProjects(email, null);

        assertThat(result.getProjects()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1L);
    }

    @Test
    void getProjectById_WhenExists_ShouldReturnProject() {
        given(projectAuthorizationService.verifyAccess(sampleProject.getId(), email, ProjectRole.VIEWER)).willReturn(sampleProject);

        ProjectResponseDto result = projectService.getProjectById(sampleProject.getId(), email);

        assertThat(result.getId()).isEqualTo(sampleProject.getId());
        assertThat(result.getName()).isEqualTo("TripNest");
    }

    @Test
    void getProjectById_WhenNotFound_ShouldThrowException() {
        UUID randomId = UUID.randomUUID();
        given(projectAuthorizationService.verifyAccess(randomId, email, ProjectRole.VIEWER)).willThrow(new ProjectNotFoundException("Project not found"));

        assertThatThrownBy(() -> projectService.getProjectById(randomId, email))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    void deleteProject_ShouldCallRepositoryDelete() {
        given(userRepository.findByEmail(email)).willReturn(Optional.of(sampleUser));
        given(projectAuthorizationService.verifyAccess(sampleProject.getId(), email, ProjectRole.OWNER)).willReturn(sampleProject);

        projectService.deleteProject(sampleProject.getId(), email);

        verify(projectRepository).delete(sampleProject);
    }
}
