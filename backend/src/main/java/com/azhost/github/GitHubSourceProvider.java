package com.azhost.github;

import com.azhost.entity.Project;
import com.azhost.entity.ProjectSourceType;
import com.azhost.github.entity.GitHubConnectionEntity;
import com.azhost.github.exception.GitHubConnectionNotFoundException;
import com.azhost.github.exception.GitHubSourceAcquisitionException;
import com.azhost.github.repository.GitHubConnectionRepository;
import com.azhost.github.security.GitHubTokenEncryptor;
import com.azhost.source.SourceAcquisitionResult;
import com.azhost.source.SourceProvider;
import com.azhost.source.ZipSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class GitHubSourceProvider implements SourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(GitHubSourceProvider.class);

    private final GitHubConnectionRepository connectionRepository;
    private final GitHubTokenEncryptor tokenEncryptor;
    private final RestTemplate restTemplate;

    @Value("${azhost.github.api-base-url:https://api.github.com}")
    private String apiBaseUrl;

    public GitHubSourceProvider(
            GitHubConnectionRepository connectionRepository,
            GitHubTokenEncryptor tokenEncryptor,
            RestTemplate restTemplate
    ) {
        this.connectionRepository = connectionRepository;
        this.tokenEncryptor = tokenEncryptor;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean supports(Project project) {
        return project.getSourceType() == ProjectSourceType.GITHUB;
    }

    @Override
    public SourceAcquisitionResult acquireSource(Project project, Path targetWorkspaceDir) throws IOException {
        if (project.getGithubRepositoryId() == null) {
            throw new GitHubSourceAcquisitionException("Project is not linked to a GitHub repository");
        }

        String commitRef = project.getGithubCommitSha();
        if (commitRef == null || commitRef.isBlank()) {
            commitRef = project.getGithubBranch();
        }
        if (commitRef == null || commitRef.isBlank()) {
            throw new GitHubSourceAcquisitionException("No valid GitHub commit SHA or branch specified for project source acquisition");
        }

        GitHubConnectionEntity connection = connectionRepository.findByUserId(project.getUser().getId())
                .orElseThrow(() -> new GitHubConnectionNotFoundException("User does not have a connected GitHub account"));

        String decryptedToken = tokenEncryptor.decrypt(connection.getEncryptedAccessToken());
        String archiveUrl = apiBaseUrl + "/repositories/" + project.getGithubRepositoryId() + "/zipball/" + commitRef;

        logger.info("Acquiring GitHub archive from URL for project '{}' (Commit: {})", project.getName(), commitRef);

        try {
            return restTemplate.execute(
                    archiveUrl,
                    HttpMethod.GET,
                    (ClientHttpRequest request) -> {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + decryptedToken);
                        request.getHeaders().set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    },
                    response -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new GitHubSourceAcquisitionException("GitHub archive download failed with HTTP status: " + response.getStatusCode());
                        }
                        try (InputStream inputStream = response.getBody()) {
                            return extractGitHubZipArchive(inputStream, targetWorkspaceDir);
                        }
                    }
            );
        } catch (Exception e) {
            if (e instanceof GitHubSourceAcquisitionException gse) throw gse;
            logger.error("Failed to acquire GitHub repository source for project '{}'", project.getName(), e);
            throw new GitHubSourceAcquisitionException("GitHub repository source acquisition failed: " + e.getMessage(), e);
        }
    }

    public SourceAcquisitionResult extractGitHubZipArchive(InputStream zipStream, Path targetWorkspaceDir) throws IOException {
        long fileCount = 0;
        long totalBytes = 0;

        String rootDirPrefix = null;

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String rawName = entry.getName();

                // Determine top-level GitHub folder prefix (e.g. "username-repo-sha/")
                if (rootDirPrefix == null) {
                    int firstSlashIndex = rawName.indexOf('/');
                    if (firstSlashIndex != -1) {
                        rootDirPrefix = rawName.substring(0, firstSlashIndex + 1);
                    }
                }

                String relativeName = rawName;
                if (rootDirPrefix != null && relativeName.startsWith(rootDirPrefix)) {
                    relativeName = relativeName.substring(rootDirPrefix.length());
                }

                if (relativeName.isBlank()) {
                    zis.closeEntry();
                    continue;
                }

                // Security checks: Reject path traversal, absolute paths, drive letters, etc.
                if (relativeName.startsWith("/") || relativeName.startsWith("\\") || relativeName.contains(":") || relativeName.contains("..")) {
                    throw new SecurityException("Illegal ZIP entry path detected: " + relativeName);
                }

                Path resolvedPath = targetWorkspaceDir.resolve(relativeName).normalize();
                if (!resolvedPath.startsWith(targetWorkspaceDir)) {
                    throw new SecurityException("ZIP Slip path traversal attack blocked for entry: " + relativeName);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolvedPath);
                } else {
                    fileCount++;
                    if (fileCount > ZipSourceProvider.MAX_ZIP_FILE_COUNT) {
                        throw new SecurityException("Archive exceeds maximum file count limit of " + ZipSourceProvider.MAX_ZIP_FILE_COUNT);
                    }

                    if (resolvedPath.getParent() != null && !Files.exists(resolvedPath.getParent())) {
                        Files.createDirectories(resolvedPath.getParent());
                    }

                    byte[] buffer = new byte[8192];
                    int len;
                    try (var os = Files.newOutputStream(resolvedPath)) {
                        while ((len = zis.read(buffer)) > 0) {
                            totalBytes += len;
                            if (totalBytes > ZipSourceProvider.MAX_UNCOMPRESSED_SIZE_BYTES) {
                                throw new SecurityException("Uncompressed archive size exceeds limit of 500 MB");
                            }
                            os.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        logger.info("Extracted GitHub repository archive into workspace: {} files, {} bytes", fileCount, totalBytes);
        return new SourceAcquisitionResult(targetWorkspaceDir, ProjectSourceType.GITHUB, fileCount, totalBytes);
    }
}
