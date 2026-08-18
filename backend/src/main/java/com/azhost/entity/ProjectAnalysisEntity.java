package com.azhost.entity;

import com.azhost.analysis.DetectionConfidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_analysis")
public class ProjectAnalysisEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Id
    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProjectFramework framework;

    @Enumerated(EnumType.STRING)
    @Column(name = "framework_confidence", nullable = false, length = 20)
    private DetectionConfidence frameworkConfidence;

    @Column(name = "build_tool", length = 50)
    private String buildTool;

    @Column(name = "package_manager", nullable = false, length = 50)
    private String packageManager;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_manager_confidence", nullable = false, length = 20)
    private DetectionConfidence packageManagerConfidence;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "build_command")
    private String buildCommand;

    @Column(name = "dev_command")
    private String devCommand;

    @Column(name = "output_directory", nullable = false, length = 100)
    private String outputDirectory;

    @Column(name = "node_version", length = 50)
    private String nodeVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DetectionConfidence confidence;

    @Column(nullable = false)
    private boolean executed = false;

    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "detected_files", columnDefinition = "TEXT")
    private String detectedFiles;

    @UpdateTimestamp
    @Column(name = "analyzed_at", nullable = false)
    private ZonedDateTime analyzedAt;

    public ProjectAnalysisEntity() {}

    public ProjectAnalysisEntity(Project project) {
        this.project = project;
        this.projectId = project.getId();
        this.executed = false;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
        if (project != null) {
            this.projectId = project.getId();
        }
    }

    public ProjectFramework getFramework() {
        return framework;
    }

    public void setFramework(ProjectFramework framework) {
        this.framework = framework;
    }

    public DetectionConfidence getFrameworkConfidence() {
        return frameworkConfidence;
    }

    public void setFrameworkConfidence(DetectionConfidence frameworkConfidence) {
        this.frameworkConfidence = frameworkConfidence;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(String buildTool) {
        this.buildTool = buildTool;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager;
    }

    public DetectionConfidence getPackageManagerConfidence() {
        return packageManagerConfidence;
    }

    public void setPackageManagerConfidence(DetectionConfidence packageManagerConfidence) {
        this.packageManagerConfidence = packageManagerConfidence;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getDevCommand() {
        return devCommand;
    }

    public void setDevCommand(String devCommand) {
        this.devCommand = devCommand;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getNodeVersion() {
        return nodeVersion;
    }

    public void setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
    }

    public DetectionConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(DetectionConfidence confidence) {
        this.confidence = confidence;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = false;
    }

    public List<String> getEvidenceList() {
        return parseJsonList(this.evidence);
    }

    public void setEvidenceList(List<String> evidenceList) {
        this.evidence = toJson(evidenceList);
    }

    public List<String> getWarningsList() {
        return parseJsonList(this.warnings);
    }

    public void setWarningsList(List<String> warningsList) {
        this.warnings = toJson(warningsList);
    }

    public List<String> getDetectedFilesList() {
        return parseJsonList(this.detectedFiles);
    }

    public void setDetectedFilesList(List<String> detectedFilesList) {
        this.detectedFiles = toJson(detectedFilesList);
    }

    public ZonedDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(ZonedDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }

    private static List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of(json);
        }
    }

    private static String toJson(List<String> list) {
        if (list == null) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
