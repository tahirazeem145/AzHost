package com.azhost.service;

import com.azhost.analysis.ProjectAnalysisResult;
import com.azhost.analysis.ProjectAnalyzer;
import com.azhost.analysis.source.LocalDirectorySourceReader;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.dto.ProjectAnalysisResponseDto;
import com.azhost.entity.Project;
import com.azhost.entity.ProjectAnalysisEntity;
import com.azhost.entity.User;
import com.azhost.exception.ProjectNotFoundException;
import com.azhost.exception.ProjectSourceNotAvailableException;
import com.azhost.repository.ProjectAnalysisRepository;
import com.azhost.repository.ProjectRepository;
import com.azhost.repository.UserRepository;
import com.azhost.source.SourceAcquisitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;


@Service
public class ProjectAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectAnalysisService.class);

    private final ProjectRepository projectRepository;
    private final ProjectAnalysisRepository projectAnalysisRepository;
    private final UserRepository userRepository;
    private final ProjectAnalyzer projectAnalyzer;

    private final SourceAcquisitionService sourceAcquisitionService;

    @Value("${azhost.analysis.allowed-root-dir:./test-fixtures}")
    private String allowedRootDir;

    public ProjectAnalysisService(
            ProjectRepository projectRepository,
            ProjectAnalysisRepository projectAnalysisRepository,
            UserRepository userRepository,
            ProjectAnalyzer projectAnalyzer,
            SourceAcquisitionService sourceAcquisitionService
    ) {
        this.projectRepository = projectRepository;
        this.projectAnalysisRepository = projectAnalysisRepository;
        this.userRepository = userRepository;
        this.projectAnalyzer = projectAnalyzer;
        this.sourceAcquisitionService = sourceAcquisitionService;
    }

    @Transactional
    public ProjectAnalysisResponseDto analyzeProject(UUID projectId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        ProjectSourceReader sourceReader;
        Path tempAnalysisDir = null;

        try {
            if (project.getSourceType() == com.azhost.entity.ProjectSourceType.GITHUB) {
                tempAnalysisDir = Files.createTempDirectory("azhost-analysis-");
                sourceAcquisitionService.acquireSource(project, tempAnalysisDir);
                sourceReader = new LocalDirectorySourceReader(tempAnalysisDir);
            } else {
                Path baseRoot = Paths.get(allowedRootDir).normalize().toAbsolutePath();
                Path candidatePathBySlug = baseRoot.resolve(project.getSlug()).normalize();
                Path candidatePathById = baseRoot.resolve(project.getId().toString()).normalize();

                sourceReader = new LocalDirectorySourceReader(candidatePathBySlug);
                if (!sourceReader.exists() || sourceReader.listRootFiles().isEmpty()) {
                    sourceReader = new LocalDirectorySourceReader(candidatePathById);
                }
            }

            if (!sourceReader.exists() || sourceReader.listRootFiles().isEmpty()) {
                logger.info("Project source not available for project '{}' (ID: {}). Returning 409 Conflict.", project.getName(), projectId);
                throw new ProjectSourceNotAvailableException("Project source is not available for analysis yet.");
            }

            // Perform safe metadata analysis
            ProjectAnalysisResult result = projectAnalyzer.analyze(sourceReader);

            // Update Project framework if detection confidence is HIGH
            if (result.getFramework() != null && result.getFrameworkConfidence() == com.azhost.analysis.DetectionConfidence.HIGH) {
                project.setFramework(result.getFramework());
                projectRepository.save(project);
            }

            // Save or update ProjectAnalysisEntity
            ProjectAnalysisEntity entity = projectAnalysisRepository.findByProjectId(projectId)
                    .orElseGet(() -> new ProjectAnalysisEntity(project));

            entity.setFramework(result.getFramework());
            entity.setFrameworkConfidence(result.getFrameworkConfidence());
            entity.setBuildTool(result.getBuildTool());
            entity.setPackageManager(result.getPackageManager());
            entity.setPackageManagerConfidence(result.getPackageManagerConfidence());
            entity.setLanguage(result.getLanguage());
            entity.setBuildCommand(result.getBuildCommand());
            entity.setDevCommand(result.getDevCommand());
            entity.setOutputDirectory(result.getOutputDirectory());
            entity.setNodeVersion(result.getNodeVersion());
            entity.setConfidence(result.getConfidence());
            entity.setExecuted(false);
            entity.setEvidenceList(result.getEvidence());
            entity.setWarningsList(result.getWarnings());
            entity.setDetectedFilesList(result.getDetectedFiles());

            ProjectAnalysisEntity savedEntity = projectAnalysisRepository.save(entity);
            logger.info("Successfully analyzed and persisted analysis for project '{}' (ID: {})", project.getName(), projectId);

            return new ProjectAnalysisResponseDto(savedEntity);
        } catch (IOException e) {
            logger.error("Failed to prepare source for project analysis", e);
            throw new ProjectSourceNotAvailableException("Failed to acquire project source for analysis: " + e.getMessage());
        } finally {
            if (tempAnalysisDir != null) {
                try {
                    deleteDirectoryRecursively(tempAnalysisDir);
                } catch (Exception e) {
                    logger.warn("Failed to delete temp analysis directory: {}", tempAnalysisDir, e);
                }
            }
        }
    }

    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }


    @Transactional(readOnly = true)
    public ProjectAnalysisResponseDto getLatestAnalysis(UUID projectId, String userEmail) {
        User user = getUser(userEmail);
        Project project = projectRepository.findByIdAndUserId(projectId, user.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Project not found with ID: " + projectId));

        ProjectAnalysisEntity entity = projectAnalysisRepository.findByProjectId(project.getId())
                .orElseThrow(() -> new ProjectNotFoundException("Analysis result not found for project ID: " + projectId));

        return new ProjectAnalysisResponseDto(entity);
    }

    private User getUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("User context not found for email: " + userEmail));
    }
}
