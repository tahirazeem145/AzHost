package com.azhost.service;

import com.azhost.config.AzHostBuildProperties;
import com.azhost.dto.CreateProjectRequest;
import com.azhost.dto.ProjectResponseDto;
import com.azhost.entity.*;
import com.azhost.repository.ProjectMemberRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class MultiTenantAuthorizationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectAuthorizationService authorizationService;

    @Autowired
    private StorageQuotaService storageQuotaService;

    @Autowired
    private AzHostBuildProperties buildProperties;

    private User userA;
    private User userB;
    private User viewerUser;
    private Project projectA;

    @BeforeEach
    public void setup() {
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Setup users
        userA = new User("userA@azhost.dev", "password_hash_A", "User A");
        userB = new User("userB@azhost.dev", "password_hash_B", "User B");
        viewerUser = new User("viewer@azhost.dev", "password_hash_viewer", "Viewer User");

        userRepository.saveAll(List.of(userA, userB, viewerUser));

        // Create Project owned by User A
        projectA = new Project(
                userA,
                "Project A",
                "project-a",
                "Desc A",
                ProjectFramework.REACT,
                ProjectSourceType.LOCAL
        );
        projectRepository.save(projectA);
    }

    @Test
    public void testRoleHierarchyPermissions() {
        // Owner has full control
        ProjectRole ownerRole = authorizationService.getRoleForUser(projectA, userA.getEmail());
        assertThat(ownerRole).isEqualTo(ProjectRole.OWNER);
        assertThat(ownerRole.satisfies(ProjectRole.OWNER)).isTrue();
        assertThat(ownerRole.satisfies(ProjectRole.MEMBER)).isTrue();
        assertThat(ownerRole.satisfies(ProjectRole.VIEWER)).isTrue();

        // Non-member has no role
        ProjectRole guestRole = authorizationService.getRoleForUser(projectA, userB.getEmail());
        assertThat(guestRole).isNull();

        // Grant Member role
        ProjectMemberEntity member = new ProjectMemberEntity(projectA, userB, ProjectRole.MEMBER);
        projectMemberRepository.save(member);

        ProjectRole userBRole = authorizationService.getRoleForUser(projectA, userB.getEmail());
        assertThat(userBRole).isEqualTo(ProjectRole.MEMBER);
        assertThat(userBRole.satisfies(ProjectRole.OWNER)).isFalse();
        assertThat(userBRole.satisfies(ProjectRole.MEMBER)).isTrue();
        assertThat(userBRole.satisfies(ProjectRole.VIEWER)).isTrue();

        // Grant Viewer role
        ProjectMemberEntity viewer = new ProjectMemberEntity(projectA, viewerUser, ProjectRole.VIEWER);
        projectMemberRepository.save(viewer);

        ProjectRole viewerRole = authorizationService.getRoleForUser(projectA, viewerUser.getEmail());
        assertThat(viewerRole).isEqualTo(ProjectRole.VIEWER);
        assertThat(viewerRole.satisfies(ProjectRole.MEMBER)).isFalse();
        assertThat(viewerRole.satisfies(ProjectRole.VIEWER)).isTrue();
    }

    @Test
    public void testCrossTenantAccessRejection() {
        // User B tries to read User A's project: verifyAccess throws AccessDeniedException
        assertThatThrownBy(() -> authorizationService.verifyAccess(projectA.getId(), userB.getEmail(), ProjectRole.VIEWER))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    public void testDevTestHeaderAuthenticationOverride() {
        // Perform call with X-User-Email header impersonating userA
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Email", userA.getEmail());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ProjectResponseDto> response = restTemplate.exchange(
                "http://localhost:" + port + "/api/projects/" + projectA.getId(),
                HttpMethod.GET,
                entity,
                ProjectResponseDto.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Project A");
    }

    @Test
    public void testRateLimitingTriggers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Email", userA.getEmail());
        HttpEntity<CreateProjectRequest> entity = new HttpEntity<>(new CreateProjectRequest(
                "Rate Limit Project",
                "Desc",
                ProjectFramework.REACT,
                ProjectSourceType.LOCAL,
                null,
                null
        ), headers);

        // Rapid POST requests to project creation to hit 5 requests/minute limit
        int rejections = 0;
        for (int i = 0; i < 10; i++) {
            ResponseEntity<ProjectResponseDto> response = restTemplate.exchange(
                    "http://localhost:" + port + "/api/projects",
                    HttpMethod.POST,
                    entity,
                    ProjectResponseDto.class
            );
            if (response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                rejections++;
            }
        }
        assertThat(rejections).isGreaterThan(0);
    }

    @Test
    public void testStorageQuotaChecking() {
        // Reserve space beyond configured limits (500MB max)
        long exceedingSize = 600 * 1024 * 1024L; // 600MB
        boolean reserved = storageQuotaService.reserveSpace(projectA.getId(), exceedingSize);
        assertThat(reserved).isFalse();

        // Release reservation and try with valid size
        boolean validReservation = storageQuotaService.reserveSpace(projectA.getId(), 10 * 1024 * 1024L); // 10MB
        assertThat(validReservation).isTrue();
        storageQuotaService.releaseReservation(projectA.getId(), 10 * 1024 * 1024L);
    }
}
