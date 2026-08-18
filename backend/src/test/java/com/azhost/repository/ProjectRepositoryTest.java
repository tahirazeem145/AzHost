package com.azhost.repository;

import com.azhost.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        User user = new User("dev@azhost.dev", "hash", "Developer");
        sampleUser = userRepository.saveAndFlush(user);
    }

    @Test
    void saveAndFindProjectBySlug_ShouldSucceed() {
        Project project = new Project(
                sampleUser,
                "TripNest",
                "tripnest",
                "Travel platform",
                ProjectFramework.REACT,
                ProjectSourceType.GITHUB,
                "https://github.com/example/tripnest",
                "main"
        );

        Project saved = projectRepository.saveAndFlush(project);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Project> found = projectRepository.findBySlugAndUserId("tripnest", sampleUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TripNest");
    }

    @Test
    void searchByUserIdAndQuery_ShouldReturnMatchingProjects() {
        Project project1 = new Project(sampleUser, "TripNest", "tripnest", "Travel platform", ProjectFramework.REACT, ProjectSourceType.GITHUB, null, null);
        Project project2 = new Project(sampleUser, "DocPortal", "docportal", "Internal documentation", ProjectFramework.VITE, ProjectSourceType.LOCAL, null, null);

        projectRepository.saveAndFlush(project1);
        projectRepository.saveAndFlush(project2);

        List<Project> searchResults = projectRepository.searchByUserIdAndQuery(sampleUser.getId(), "trip");
        assertThat(searchResults).hasSize(1);
        assertThat(searchResults.get(0).getName()).isEqualTo("TripNest");
    }
}
