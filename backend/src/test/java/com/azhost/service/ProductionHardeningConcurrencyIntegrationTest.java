package com.azhost.service;

import com.azhost.build.BuildLogStreamer;
import com.azhost.build.BuildStatus;
import com.azhost.build.executor.BuildExecutor;
import com.azhost.build.executor.BuildResult;
import com.azhost.build.workspace.BuildWorkspaceManager;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(ProductionHardeningConcurrencyIntegrationTest.TestConfig.class)
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
    private com.azhost.deployment.DeploymentManager deploymentManager;

    @Autowired
    private AzHostBuildProperties buildProperties;

    @Autowired
    private BuildWorkspaceManager buildWorkspaceManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private User user;
    private Project projectA;
    private Project projectB;
    private Project projectC;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        @Primary
        public BuildExecutor testBuildExecutor() {
            return new TestBuildExecutor();
        }
    }

    public static class TestBuildExecutor implements BuildExecutor {
        public static CountDownLatch startLatch = new CountDownLatch(1);
        public static CountDownLatch activeBuildsLatch = new CountDownLatch(2);
        public static final AtomicInteger activeBuildsCount = new AtomicInteger(0);
        public static final AtomicInteger maxObservedConcurrency = new AtomicInteger(0);
        public static final List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());
        public static boolean useLatch = false;

        @Override
        public boolean isDockerAvailable() {
            return false;
        }

        @Override
        public BuildResult executeBuild(
                UUID buildId,
                Path workspacePath,
                ProjectAnalysisEntity analysis,
                BuildLogStreamer logStreamer
        ) {
            if (useLatch) {
                executionOrder.add(analysis.getProject().getName() + "-" + buildId);
                int current = activeBuildsCount.incrementAndGet();
                synchronized (maxObservedConcurrency) {
                    if (current > maxObservedConcurrency.get()) {
                        maxObservedConcurrency.set(current);
                    }
                }
                activeBuildsLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                activeBuildsCount.decrementAndGet();
            }
            return BuildResult.success(100L, "artifact-" + buildId, "/tmp/art");
        }

        @Override
        public void cancelBuild(UUID buildId) {
        }
    }

    private static void createDummyZipWithIndexHtml(Path zipPath) throws Exception {
        try (var fos = new java.io.FileOutputStream(zipPath.toFile());
             var zos = new java.util.zip.ZipOutputStream(fos)) {
            var entry = new java.util.zip.ZipEntry("index.html");
            zos.putNextEntry(entry);
            zos.write("<html><body>Test</body></html>".getBytes());
            zos.closeEntry();
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        buildManager.reset();
        deploymentManager.reset();
        deploymentRepository.deleteAll();
        buildRepository.deleteAll();
        analysisRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = new User("test-concurrency@azhost.dev", "hash", "Concurrency User");
        user = userRepository.saveAndFlush(user);

        projectA = new Project(user, "ProjectA", "project-a", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectA = projectRepository.saveAndFlush(projectA);

        projectB = new Project(user, "ProjectB", "project-b", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectB = projectRepository.saveAndFlush(projectB);

        projectC = new Project(user, "ProjectC", "project-c", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "http://github.com", "main");
        projectC = projectRepository.saveAndFlush(projectC);

        String sql = "INSERT INTO project_analysis (project_id, framework, framework_confidence, language, package_manager, package_manager_confidence, confidence, executed, output_directory, node_version, build_command, analyzed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, projectA.getId(), "STATIC", "HIGH", "JavaScript", "NPM", "HIGH", "HIGH", false, "dist", "20", "npm run build", new java.sql.Timestamp(System.currentTimeMillis()));
        jdbcTemplate.update(sql, projectB.getId(), "STATIC", "HIGH", "JavaScript", "NPM", "HIGH", "HIGH", false, "dist", "20", "npm run build", new java.sql.Timestamp(System.currentTimeMillis()));
        jdbcTemplate.update(sql, projectC.getId(), "STATIC", "HIGH", "JavaScript", "NPM", "HIGH", "HIGH", false, "dist", "20", "npm run build", new java.sql.Timestamp(System.currentTimeMillis()));

        // Write mock artifacts to disk so validation passes
        Path artifactsRoot = buildWorkspaceManager.getArtifactsRoot();
        if (!Files.exists(artifactsRoot)) {
            Files.createDirectories(artifactsRoot);
        }
        createDummyZipWithIndexHtml(artifactsRoot.resolve("artifact-A.zip"));
        createDummyZipWithIndexHtml(artifactsRoot.resolve("artifact-B.zip"));

        TestBuildExecutor.startLatch = new CountDownLatch(1);
        TestBuildExecutor.activeBuildsLatch = new CountDownLatch(2);
        TestBuildExecutor.activeBuildsCount.set(0);
        TestBuildExecutor.maxObservedConcurrency.set(0);
        TestBuildExecutor.executionOrder.clear();
        TestBuildExecutor.useLatch = false;
    }

    @Test
    public void staleDeploymentRaceTest() throws Exception {
        // 1. Create build A and deployment A
        ProjectBuildEntity buildA = new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-A");
        buildA.setStatus(BuildStatus.SUCCESS);
        buildA.setArtifactId("artifact-A");
        buildA = buildRepository.save(buildA);

        CreateDeploymentRequest reqA = new CreateDeploymentRequest();
        reqA.setBuildId(buildA.getId());
        var depAResp = deploymentService.createDeployment(projectA.getId(), reqA, user.getEmail());
        DeploymentEntity depA = deploymentRepository.findById(depAResp.getId()).orElseThrow();

        // Release lock after submission to allow B to start
        deploymentManager.reset();

        // 2. Create build B and deployment B
        ProjectBuildEntity buildB = new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "npm run build", "dist", "ws-B");
        buildB.setStatus(BuildStatus.SUCCESS);
        buildB.setArtifactId("artifact-B");
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

        TestBuildExecutor.useLatch = true;

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
        TestBuildExecutor.activeBuildsLatch.await(5, TimeUnit.SECONDS);

        // Verify that only 2 builds ran concurrently
        assertThat(TestBuildExecutor.maxObservedConcurrency.get()).isEqualTo(2);

        // Verify that Project A's b4 did NOT start because Project A's b1 is active (serialized per project)
        // Verify that Project B's b5 did NOT start because Project B's b2 is active
        assertThat(TestBuildExecutor.executionOrder).hasSize(2);
        assertThat(TestBuildExecutor.executionOrder.get(0)).startsWith("ProjectA");
        assertThat(TestBuildExecutor.executionOrder.get(1)).startsWith("ProjectB");

        // Release the first 2 builds
        TestBuildExecutor.startLatch.countDown();

        // Wait a short moment for completion and processing of remainder
        Thread.sleep(1000);

        // Verify remaining builds execute sequentially and respect project serialization FIFO
        assertThat(TestBuildExecutor.executionOrder).hasSize(5);

        // Assert B4 (Project A) is after B1 (Project A)
        int idxB1 = -1, idxB4 = -1;
        for (int i = 0; i < TestBuildExecutor.executionOrder.size(); i++) {
            if (TestBuildExecutor.executionOrder.get(i).contains(b1.getId().toString())) idxB1 = i;
            if (TestBuildExecutor.executionOrder.get(i).contains(b4.getId().toString())) idxB4 = i;
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
