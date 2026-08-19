package com.azhost.service;

import com.azhost.deployment.DeploymentStatus;
import com.azhost.entity.DeploymentEntity;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.repository.DeploymentRepository;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ProductionHardeningIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectBuildRepository buildRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeploymentService deploymentService;

    private User user;
    private Project project;

    @BeforeEach
    @Transactional
    public void setUp() {
        user = new User("test-hardening@azhost.dev", "hash", "Hardening User");
        user = userRepository.save(user);

        project = new Project(user, "HardeningProject", "hardening-project", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        project = projectRepository.save(project);
    }

    @Test
    @Transactional
    public void staleDeploymentOrderingProtectionTest() throws Exception {
        // Simulation details:
        // 1. Create build A and deployment A (created first)
        ProjectBuildEntity buildA = new ProjectBuildEntity(project, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-A");
        buildA = buildRepository.save(buildA);
        DeploymentEntity depA = new DeploymentEntity(project, buildA, "art-A");
        depA.setStatus(DeploymentStatus.SUCCESS);
        depA.setCreatedAt(ZonedDateTime.now().minusMinutes(5));
        depA = deploymentRepository.save(depA);

        // 2. Create build B and deployment B (created later)
        ProjectBuildEntity buildB = new ProjectBuildEntity(project, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-B");
        buildB = buildRepository.save(buildB);
        DeploymentEntity depB = new DeploymentEntity(project, buildB, "art-B");
        depB.setStatus(DeploymentStatus.SUCCESS);
        depB.setCreatedAt(ZonedDateTime.now());
        depB = deploymentRepository.save(depB);

        // 3. Deployment B completes first and promotes
        deploymentService.setActiveDeploymentForProject(project.getId(), depB.getId());
        
        Project projectReloaded = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(projectReloaded.getActiveDeployment().getId()).isEqualTo(depB.getId());

        // 4. Deployment A completes later and tries to promote
        deploymentService.setActiveDeploymentForProject(project.getId(), depA.getId());

        // 5. Candidate deployment A must NOT replace deployment B because it was created before B
        projectReloaded = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(projectReloaded.getActiveDeployment().getId()).isEqualTo(depB.getId());
    }

    @Test
    @Transactional
    public void failedBuildRetentionLiveSafetyTest() {
        // Simulation details:
        // 1. Create build A and deployment A (successfully live)
        ProjectBuildEntity buildA = new ProjectBuildEntity(project, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-A");
        buildA = buildRepository.save(buildA);
        DeploymentEntity depA = new DeploymentEntity(project, buildA, "art-A");
        depA.setStatus(DeploymentStatus.SUCCESS);
        depA.setCreatedAt(ZonedDateTime.now());
        depA = deploymentRepository.save(depA);

        deploymentService.setActiveDeploymentForProject(project.getId(), depA.getId());

        // 2. Simulate build B failing
        ProjectBuildEntity buildB = new ProjectBuildEntity(project, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-B");
        buildB.setStatus(com.azhost.build.BuildStatus.FAILED);
        buildB = buildRepository.save(buildB);

        // 3. Current Live must remain Deployment A
        Project projectReloaded = projectRepository.findById(project.getId()).orElseThrow();
        assertThat(projectReloaded.getActiveDeployment().getId()).isEqualTo(depA.getId());
    }
}
