package com.azhost.service;

import com.azhost.build.BuildClaimService;
import com.azhost.build.BuildStatus;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectBuildEntity;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class DistributedBuildClaimIntegrationTest {

    @Autowired
    private ProjectBuildRepository buildRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BuildClaimService buildClaimService;

    private User user;
    private Project projectA;
    private Project projectB;

    @BeforeEach
    public void setUp() {
        buildRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.saveAndFlush(new User("dist-user@azhost.dev", "hash", "Dist User"));
        projectA = projectRepository.saveAndFlush(new Project(user, "Project A", "proj-a", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "url", "main"));
        projectB = projectRepository.saveAndFlush(new Project(user, "Project B", "proj-b", "desc", ProjectFramework.STATIC, ProjectSourceType.GITHUB, "url", "main"));
    }

    @Test
    public void testAtomicClaimByMultipleWorkersNoDuplicateClaims() throws Exception {
        // Create 10 queued builds for Project A and Project B
        for (int i = 0; i < 5; i++) {
            buildRepository.save(new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "build", "dist", "ws-a-" + i));
            buildRepository.save(new ProjectBuildEntity(projectB, ProjectFramework.STATIC, "NPM", "20", "build", "dist", "ws-b-" + i));
        }

        // Simulate 4 concurrent worker threads attempting to claim builds simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Callable<ProjectBuildEntity>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final String workerId = "worker-node-" + (i % 4);
            tasks.add(() -> buildClaimService.claimNextBuild(workerId));
        }

        List<Future<ProjectBuildEntity>> results = executor.invokeAll(tasks);
        executor.shutdown();

        List<UUID> claimedBuildIds = new ArrayList<>();
        for (Future<ProjectBuildEntity> future : results) {
            ProjectBuildEntity entity = future.get();
            if (entity != null) {
                assertThat(claimedBuildIds).doesNotContain(entity.getId());
                claimedBuildIds.add(entity.getId());
            }
        }

        // Only 2 builds total should be claimed at first (1 for Project A and 1 for Project B)
        // because of per-project single active build serialization!
        assertThat(claimedBuildIds).hasSize(2);
    }

    @Test
    public void testStaleHeartbeatRecovery() {
        ProjectBuildEntity build = new ProjectBuildEntity(projectA, ProjectFramework.STATIC, "NPM", "20", "build", "dist", "ws-stale");
        build.setStatus(BuildStatus.BUILDING);
        build.setClaimedBy("dead-worker-1");
        build.setClaimedAt(ZonedDateTime.now().minusMinutes(10));
        build.setHeartbeatAt(ZonedDateTime.now().minusMinutes(5));
        buildRepository.saveAndFlush(build);

        ZonedDateTime threshold = ZonedDateTime.now().minusMinutes(1);
        List<ProjectBuildEntity> staleBuilds = buildRepository.findStaleClaimedBuilds(threshold);

        assertThat(staleBuilds).hasSize(1);
        assertThat(staleBuilds.get(0).getId()).isEqualTo(build.getId());
    }
}
