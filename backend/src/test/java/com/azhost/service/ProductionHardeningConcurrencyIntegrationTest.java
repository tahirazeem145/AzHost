package com.azhost.service;

import com.azhost.build.BuildStatus;
import com.azhost.build.executor.BuildExecutor;
import com.azhost.build.executor.BuildResult;
import com.azhost.config.AzHostBuildProperties;
import com.azhost.deployment.DeploymentStatus;
import com.azhost.dto.CreateDeploymentRequest;
import com.azhost.entity.*;
import com.azhost.exception.BuildQueueFullException;
import com.azhost.repository.DeploymentRepository;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class ProductionHardeningConcurrencyIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectBuildRepository buildRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private com.azhost.repository.ProjectAnalysisRepository analysisRepository;

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private com.azhost.build.BuildManager buildManager;

    @Autowired
    private AzHostBuildProperties buildProperties;

    @MockBean
    private BuildExecutor buildExecutor;

    private User user;
    private Project projectA;
    private Project projectB;
    private Project projectC;

    @BeforeEach
    public void setUp() {
        buildManager.reset();
        deploymentRepository.deleteAll();
        buildRepository.deleteAll();
        analysisRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.saveAndFlush(user);

        projectA = new Project(user, "ProjectA", "project-a", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectA = projectRepository.saveAndFlush(projectA);

        projectB = new Project(user, "ProjectB", "project-b", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectB = projectRepository.saveAndFlush(projectB);

        projectC = new Project(user, "ProjectC", "project-c", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectC = projectRepository.saveAndFlush(projectC);

        ProjectAnalysisEntity analysisA = new ProjectAnalysisEntity(projectA);
        analysisA.setFramework(ProjectFramework.STATIC);
        analysisA.setFrameworkConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisA.setLanguage("JavaScript");
        analysisA.setConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisA.setPackageManager("NPM");
        analysisA.setPackageManagerConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisA.setNodeVersion("20");
        analysisA.setBuildCommand("npm run build");
        analysisA.setOutputDirectory("dist");
        analysisRepository.saveAndFlush(analysisA);

        ProjectAnalysisEntity analysisB = new ProjectAnalysisEntity(projectB);
        analysisB.setFramework(ProjectFramework.STATIC);
        analysisB.setFrameworkConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisB.setLanguage("JavaScript");
        analysisB.setConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisB.setPackageManager("NPM");
        analysisB.setPackageManagerConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisB.setNodeVersion("20");
        analysisB.setBuildCommand("npm run build");
        analysisB.setOutputDirectory("dist");
        analysisRepository.saveAndFlush(analysisB);

        ProjectAnalysisEntity analysisC = new ProjectAnalysisEntity(projectC);
        analysisC.setFramework(ProjectFramework.STATIC);
        analysisC.setFrameworkConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisC.setLanguage("JavaScript");
        analysisC.setConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisC.setPackageManager("NPM");
        analysisC.setPackageManagerConfidence(com.azhost.analysis.DetectionConfidence.HIGH);
        analysisC.setNodeVersion("20");
        analysisC.setBuildCommand("npm run build");
        analysisC.setOutputDirectory("dist");
        analysisRepository.saveAndFlush(analysisC);
    }

    @Test
    public void staleDeploymentRaceTest() throws Exception {
        // 1. Create build A and deployment A
        ProjectBuildEntity buildA = new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-A");
        buildA.setStatus(BuildStatus.SUCCESS);
        buildA = buildRepository.save(buildA);
        
        CreateDeploymentRequest reqA = new CreateDeploymentRequest();
        reqA.setBuildId(buildA.getId());
        var depAResp = deploymentService.createDeployment(projectA.getId(), reqA, user.getEmail());
        DeploymentEntity depA = deploymentRepository.findById(depAResp.getId()).orElseThrow();

        // 2. Create build B and deployment B
        ProjectBuildEntity buildB = new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-B");
        buildB.setStatus(BuildStatus.SUCCESS);
        buildB = buildRepository.save(buildB);
        
        CreateDeploymentRequest reqB = new CreateDeploymentRequest();
        reqB.setBuildId(buildB.getId());
        var depBResp = deploymentService.createDeployment(projectA.getId(), reqB, user.getEmail());
        DeploymentEntity depB = deploymentRepository.findById(depBResp.getId()).orElseThrow();

        // Assert sequences are monotonically increasing
        assertThat(depB.getSequenceNumber()).isGreaterThan(depA.getSequenceNumber());

        // 3. Deployment B completes first and promotes
        depB.setStatus(DeploymentStatus.SUCCESS);
        depB = deploymentRepository.save(depB);
        deploymentService.setActiveDeploymentForProject(projectA.getId(), depB.getId());

        Project projectReloaded = projectRepository.findById(projectA.getId()).orElseThrow();
        assertThat(projectReloaded.getActiveDeployment().getId()).isEqualTo(depB.getId());

        // 4. Deployment A completes later and tries to promote
        depA.setStatus(DeploymentStatus.SUCCESS);
        depA = deploymentRepository.save(depA);
        
        final DeploymentEntity finalDepA = depA;
        final DeploymentEntity finalDepB = depB;
        
        // Concurrent verification: attempt promotion concurrently from 2 threads
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Callable<Void>> tasks = new ArrayList<>();
        tasks.add(() -> {
            deploymentService.setActiveDeploymentForProject(projectA.getId(), finalDepA.getId());
            return null;
        });
        tasks.add(() -> {
            deploymentService.setActiveDeploymentForProject(projectA.getId(), finalDepB.getId());
            return null;
        });
        executor.invokeAll(tasks);
        executor.shutdown();

        // 5. Candidate deployment A must NOT replace deployment B because sequence number is lower
        projectReloaded = projectRepository.findById(projectA.getId()).orElseThrow();
        assertThat(projectReloaded.getActiveDeployment().getId()).isEqualTo(depB.getId());
    }

    @Test
    public void globalConcurrencyAndFIFOTest() throws Exception {
        // Configure build properties for concurrency test
        buildProperties.getBuild().setMaxConcurrentBuilds(2);
        buildProperties.getBuild().setQueueCapacity(10);
        buildManager.init(); // reinitialize thread pool

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch activeBuildsLatch = new CountDownLatch(2);
        AtomicInteger activeBuildsCount = new AtomicInteger(0);
        AtomicInteger maxObservedConcurrency = new AtomicInteger(0);

        List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

        when(buildExecutor.executeBuild(any(), any(), any(), any())).thenAnswer(invocation -> {
            UUID buildId = invocation.getArgument(0);
            ProjectBuildEntity b = buildRepository.findById(buildId).orElseThrow();
            
            executionOrder.add(b.getProject().getName() + "-" + b.getId());
            int current = activeBuildsCount.incrementAndGet();
            
            synchronized (maxObservedConcurrency) {
                if (current > maxObservedConcurrency.get()) {
                    maxObservedConcurrency.set(current);
                }
            }

            activeBuildsLatch.countDown();
            
            // Block until startLatch is released
            startLatch.await();
            
            activeBuildsCount.decrementAndGet();
            return BuildResult.success(100L, "mock-artifact-" + buildId, "/tmp/art");
        });

        // Submit 5 builds:
        // B1 -> Project A
        // B2 -> Project B
        // B3 -> Project C
        // B4 -> Project A
        // B5 -> Project B
        ProjectBuildEntity b1 = buildRepository.save(new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-1"));
        ProjectBuildEntity b2 = buildRepository.save(new ProjectBuildEntity(projectB, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-2"));
        ProjectBuildEntity b3 = buildRepository.save(new ProjectBuildEntity(projectC, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-3"));
        ProjectBuildEntity b4 = buildRepository.save(new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-4"));
        ProjectBuildEntity b5 = buildRepository.save(new ProjectBuildEntity(projectB, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-5"));

        buildManager.submitBuildTask(b1, projectA, Path.of("/tmp/ws-1"));
        buildManager.submitBuildTask(b2, projectB, Path.of("/tmp/ws-2"));
        buildManager.submitBuildTask(b3, projectC, Path.of("/tmp/ws-3"));
        buildManager.submitBuildTask(b4, projectA, Path.of("/tmp/ws-4"));
        buildManager.submitBuildTask(b5, projectB, Path.of("/tmp/ws-5"));

        // Wait for first 2 concurrent builds to start
        activeBuildsLatch.await(5, TimeUnit.SECONDS);

        // Verify that only 2 builds ran concurrently
        assertThat(maxObservedConcurrency.get()).isEqualTo(2);

        // Verify that Project A's b4 did NOT start because Project A's b1 is active (serialized per project)
        // Verify that Project B's b5 did NOT start because Project B's b2 is active
        assertThat(executionOrder).hasSize(2);
        assertThat(executionOrder.get(0)).startsWith("ProjectA");
        assertThat(executionOrder.get(1)).startsWith("ProjectB");

        // Release the first 2 builds
        startLatch.countDown();

        // Wait a short moment for completion and processing of remainder
        Thread.sleep(1000);

        // Verify remaining builds execute sequentially and respect project serialization FIFO
        assertThat(executionOrder).hasSize(5);
        
        // Assert B4 (Project A) is after B1 (Project A)
        int idxB1 = -1, idxB4 = -1;
        for (int i = 0; i < executionOrder.size(); i++) {
            if (executionOrder.get(i).contains(b1.getId().toString())) idxB1 = i;
            if (executionOrder.get(i).contains(b4.getId().toString())) idxB4 = i;
        }
        assertThat(idxB1).isLessThan(idxB4);
    }

    @Test
    public void queueCapacityOverflowTest() {
        buildProperties.getBuild().setMaxConcurrentBuilds(1);
        buildProperties.getBuild().setQueueCapacity(2);
        buildManager.init();

        // Submit builds to overflow
        ProjectBuildEntity b1 = buildRepository.save(new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-1"));
        ProjectBuildEntity b2 = buildRepository.save(new ProjectBuildEntity(projectB, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-2"));
        ProjectBuildEntity b3 = buildRepository.save(new ProjectBuildEntity(projectC, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-3"));
        ProjectBuildEntity b4 = buildRepository.save(new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-4"));

        buildManager.submitBuildTask(b1, projectA, Path.of("/tmp/ws-1"));
        buildManager.submitBuildTask(b2, projectB, Path.of("/tmp/ws-2"));
        buildManager.submitBuildTask(b3, projectC, Path.of("/tmp/ws-3"));

        assertThatThrownBy(() -> buildManager.submitBuildTask(b4, projectA, Path.of("/tmp/ws-4")))
                .isInstanceOf(BuildQueueFullException.class);
    }
}
