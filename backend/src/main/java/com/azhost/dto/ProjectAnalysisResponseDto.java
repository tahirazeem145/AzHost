package com.azhost.dto;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.ProjectAnalysisResult;
import com.azhost.entity.ProjectAnalysisEntity;
import com.azhost.entity.ProjectFramework;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class ProjectAnalysisResponseDto {

    private UUID projectId;
    private ProjectFramework framework;
    private DetectionConfidence frameworkConfidence;
    private String buildTool;
    private String packageManager;
    private DetectionConfidence packageManagerConfidence;
    private String language;
    private String buildCommand;
    private String devCommand;
    private String outputDirectory;
    private String nodeVersion;
    private DetectionConfidence confidence;
    private boolean executed = false;
    private List<String> evidence;
    private List<String> warnings;
    private List<String> detectedFiles;
    private ZonedDateTime analyzedAt;

    public ProjectAnalysisResponseDto() {}

    public ProjectAnalysisResponseDto(UUID projectId, ProjectAnalysisResult result) {
        this.projectId = projectId;
        this.framework = result.getFramework();
        this.frameworkConfidence = result.getFrameworkConfidence();
        this.buildTool = result.getBuildTool();
        this.packageManager = result.getPackageManager();
        this.packageManagerConfidence = result.getPackageManagerConfidence();
        this.language = result.getLanguage();
        this.buildCommand = result.getBuildCommand();
        this.devCommand = result.getDevCommand();
        this.outputDirectory = result.getOutputDirectory();
        this.nodeVersion = result.getNodeVersion();
        this.confidence = result.getConfidence();
        this.executed = false;
        this.evidence = result.getEvidence();
        this.warnings = result.getWarnings();
        this.detectedFiles = result.getDetectedFiles();
        this.analyzedAt = result.getAnalyzedAt();
    }

    public ProjectAnalysisResponseDto(ProjectAnalysisEntity entity) {
        this.projectId = entity.getProjectId();
        this.framework = entity.getFramework();
        this.frameworkConfidence = entity.getFrameworkConfidence();
        this.buildTool = entity.getBuildTool();
        this.packageManager = entity.getPackageManager();
        this.packageManagerConfidence = entity.getPackageManagerConfidence();
        this.language = entity.getLanguage();
        this.buildCommand = entity.getBuildCommand();
        this.devCommand = entity.getDevCommand();
        this.outputDirectory = entity.getOutputDirectory();
        this.nodeVersion = entity.getNodeVersion();
        this.confidence = entity.getConfidence();
        this.executed = false;
        this.evidence = entity.getEvidenceList();
        this.warnings = entity.getWarningsList();
        this.detectedFiles = entity.getDetectedFilesList();
        this.analyzedAt = entity.getAnalyzedAt();
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
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

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public List<String> getDetectedFiles() {
        return detectedFiles;
    }

    public void setDetectedFiles(List<String> detectedFiles) {
        this.detectedFiles = detectedFiles;
    }

    public ZonedDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(ZonedDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
