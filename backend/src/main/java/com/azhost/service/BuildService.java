package com.azhost.service;

import com.azhost.build.BuildLogStreamer;
import com.azhost.build.BuildManager;
import com.azhost.build.executor.BuildWorkspaceManager;
import com.azhost.dto.BuildLogResponseDto;
import com.azhost.dto.BuildResponseDto;
import com.azhost.entity.*;
import com.azhost.exception.BuildNotFoundException;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.exception.ProjectSourceNotAvailableException;
import com.azhost.repository.ProjectAnalysisRepository;
import com.azhost.repository.ProjectBuildRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import com.azhost.source.SourceAcquisitionResult;
import com.azhost.source.SourceAcquisitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);

    private final ProjectRepository projectRepository;
    private final ProjectAnalysisRepository projectAnalysisRepository;
    private final ProjectBuildRepository projectBuildRepository;
    private final UserRepository userRepository;
    private final ProjectAnalysisService projectAnalysisService;
    private final SourceAcquisitionService sourceAcquisitionService;
    private final BuildWorkspaceManager workspaceManager;
    private final BuildManager buildManager;
    private final ProjectAuthorizationService projectAuthorizationService;

    public BuildService(
            ProjectRepository projectRepository,
            ProjectAnalysisRepository projectAnalysisRepository,
            ProjectBuildRepository projectBuildRepository,
            UserRepository userRepository,
            ProjectAnalysisService projectAnalysisService,
            SourceAcquisitionService sourceAcquisitionService,
            BuildWorkspaceManager workspaceManager,
            BuildManager buildManager,
            ProjectAuthorizationService projectAuthorizationService
    ) {
        this.projectRepository = projectRepository;
        this.projectAnalysisRepository = projectAnalysisRepository;
        this.projectBuildRepository = projectBuildRepository;
        this.userRepository = userRepository;
        this.projectAnalysisService = projectAnalysisService;
        this.sourceAcquisitionService = sourceAcquisitionService;
        this.workspaceManager = workspaceManager;
        this.buildManager = buildManager;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    @Transactional
    public BuildResponseDto startBuild(UUID projectId, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.MEMBER);

        // Retrieve or execute metadata analysis
        ProjectAnalysisEntity analysis = projectAnalysisRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    try {
                        projectAnalysisService.analyzeProject(projectId, userEmail);
                        return projectAnalysisRepository.findByProjectId(projectId)
                                .orElseThrow(() -> new ProjectSourceNotAvailableException("Project source is not available for building yet."));
                    } catch (ProjectSourceNotAvailableException ex) {
                        throw ex;
                    } catch (Exception e) {
                        throw new ProjectSourceNotAvailableException("Project source is not available for building yet.");
                    }
                });

        // Reserve active build slot for project
        String workspaceId = workspaceManager.generateWorkspaceId();
        ProjectBuildEntity buildEntity = new ProjectBuildEntity(
                project,
                analysis.getFramework(),
                analysis.getPackageManager() != null ? analysis.getPackageManager() : "NPM",
                analysis.getNodeVersion() != null ? analysis.getNodeVersion() : "20",
                analysis.getBuildCommand(),
                analysis.getOutputDirectory() != null ? analysis.getOutputDirectory() : "dist",
                workspaceId
        );
        buildEntity.setSourceType(project.getSourceType());
        buildEntity.setGithubRepositoryId(project.getGithubRepositoryId());
        buildEntity.setGithubCommitSha(project.getGithubCommitSha());

        ProjectBuildEntity savedEntity = projectBuildRepository.save(buildEntity);

        Path workspacePath;
        try {
            workspacePath = workspaceManager.createWorkspace(workspaceId);
            SourceAcquisitionResult acquiredSource = sourceAcquisitionService.acquireSource(project, workspacePath);
            logger.info("Source acquired for build ID {}: {} files", savedEntity.getId(), acquiredSource.getTotalFileCount());
        } catch (IOException | SecurityException e) {
            savedEntity.setStatus(com.azhost.build.BuildStatus.FAILED);
            savedEntity.setErrorMessage("Source acquisition failed: " + e.getMessage());
            projectBuildRepository.save(savedEntity);
            throw new ProjectSourceNotAvailableException("Source acquisition failed: " + e.getMessage());
        }

        buildManager.submitBuildTask(savedEntity, project, workspacePath);
        return new BuildResponseDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public List<BuildResponseDto> getBuildsForProject(UUID projectId, String userEmail, int page, int size) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.VIEWER);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return projectBuildRepository.findByProjectIdOrderByCreatedAtDesc(project.getId(), pageable).getContent().stream()
                .map(BuildResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BuildResponseDto getBuildById(UUID projectId, UUID buildId, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.VIEWER);

        ProjectBuildEntity entity = projectBuildRepository.findByIdAndProjectId(buildId, project.getId())
                .orElseThrow(() -> new BuildNotFoundException("Build not found with ID: " + buildId));

        return new BuildResponseDto(entity);
    }

    @Transactional(readOnly = true)
    public BuildLogResponseDto getBuildLogs(UUID projectId, UUID buildId, String userEmail, int page, int size) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.VIEWER);

        ProjectBuildEntity entity = projectBuildRepository.findByIdAndProjectId(buildId, project.getId())
                .orElseThrow(() -> new BuildNotFoundException("Build not found with ID: " + buildId));

        int pageSize = Math.min(size, 500);
        if (pageSize <= 0) pageSize = 100;

        BuildLogStreamer streamer = buildManager.getLogStreamer(buildId);
        List<String> allLines = streamer.getLogLines();

        int fromIndex = page * pageSize;
        if (fromIndex >= allLines.size() || fromIndex < 0) {
            return new BuildLogResponseDto(entity.getId(), entity.getStatus(), List.of(), streamer.isTruncated());
        }
        int toIndex = Math.min(fromIndex + pageSize, allLines.size());
        List<String> paginatedLines = allLines.subList(fromIndex, toIndex);

        return new BuildLogResponseDto(entity.getId(), entity.getStatus(), paginatedLines, streamer.isTruncated());
    }

    @Transactional
    public BuildResponseDto cancelBuild(UUID projectId, UUID buildId, String userEmail) {
        Project project = projectAuthorizationService.verifyAccess(projectId, userEmail, ProjectRole.MEMBER);

        ProjectBuildEntity entity = projectBuildRepository.findByIdAndProjectId(buildId, project.getId())
                .orElseThrow(() -> new BuildNotFoundException("Build not found with ID: " + buildId));

        if (!entity.getStatus().isTerminal()) {
            buildManager.cancelBuild(buildId);
            entity.setStatus(com.azhost.build.BuildStatus.CANCELLED);
            projectBuildRepository.save(entity);
        }

        return new BuildResponseDto(entity);
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
