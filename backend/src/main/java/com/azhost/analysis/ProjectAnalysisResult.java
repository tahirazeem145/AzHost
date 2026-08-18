package com.azhost.analysis;

import com.azhost.entity.ProjectFramework;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectAnalysisResult {

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
    private List<String> evidence = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> detectedFiles = new ArrayList<>();
    private ZonedDateTime analyzedAt;

    public ProjectAnalysisResult() {
        this.analyzedAt = ZonedDateTime.now();
        this.executed = false;
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
        this.executed = false; // Always false by contract
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
