package com.azhost.github;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectFramework;
import com.azhost.entity.ProjectSourceType;
import com.azhost.entity.User;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.source.SourceAcquisitionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitHubSourceProviderTest {

    private GitHubConnectionRepository connectionRepository;
    private GitHubTokenEncryptor tokenEncryptor;
    private RestTemplate restTemplate;
    private GitHubSourceProvider sourceProvider;

    private User testUser;
    private Project project;

    @BeforeEach
    void setUp() {
        connectionRepository = mock(GitHubConnectionRepository.class);
        tokenEncryptor = mock(GitHubTokenEncryptor.class);
        restTemplate = mock(RestTemplate.class);

        sourceProvider = new GitHubSourceProvider(connectionRepository, tokenEncryptor, restTemplate);
        ReflectionTestUtils.setField(sourceProvider, "apiBaseUrl", "https://api.github.com");

        testUser = new User("dev@azhost.dev", "hash", "Dev");
        testUser.setId(UUID.randomUUID());

        project = new Project(testUser, "TripNest", "tripnest", "Desc", ProjectFramework.REACT, ProjectSourceType.GITHUB, "https://github.com/user/repo", "main");
        project.setGithubRepositoryId(12345L);
        project.setGithubCommitSha("abc123def456");
    }

    @Test
    void shouldSupportOnlyGitHubSourceType() {
        assertTrue(sourceProvider.supports(project));

        project.setSourceType(ProjectSourceType.LOCAL);
        assertFalse(sourceProvider.supports(project));
    }

    @Test
    void shouldExtractArchiveAndStripTopLevelPrefix(@TempDir Path tempDir) throws IOException {
        byte[] zipBytes = createMockZipArchive("octocat-TripNest-abc1234/package.json", "{\"name\":\"tripnest\"}");

        SourceAcquisitionResult result = sourceProvider.extractGitHubZipArchive(new ByteArrayInputStream(zipBytes), tempDir);

        assertEquals(1, result.getTotalFileCount());
        assertTrue(Files.exists(tempDir.resolve("package.json")), "package.json should exist directly in target workspace");
        assertEquals("{\"name\":\"tripnest\"}", Files.readString(tempDir.resolve("package.json")));
    }

    @Test
    void shouldRejectZipSlipMaliciousPath(@TempDir Path tempDir) {
        byte[] zipBytes = createMockZipArchive("octocat-TripNest-abc1234/../../etc/passwd", "root:x:0:0");

        assertThrows(SecurityException.class, () ->
                sourceProvider.extractGitHubZipArchive(new ByteArrayInputStream(zipBytes), tempDir));
    }

    @Test
    void shouldRejectAbsoluteOrDrivePath(@TempDir Path tempDir) {
        byte[] zipBytes = createMockZipArchive("octocat-TripNest-abc1234/C:/windows/system32/cmd.exe", "evil");

        assertThrows(SecurityException.class, () ->
                sourceProvider.extractGitHubZipArchive(new ByteArrayInputStream(zipBytes), tempDir));
    }

    private byte[] createMockZipArchive(String entryPath, String content) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryPath);
            zos.putNextEntry(entry);
            zos.write(content.getBytes());
            zos.closeEntry();
            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
