package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;

import java.util.List;

public interface FrameworkDetector {

    class FrameworkDetectionResult {
        private final boolean detected;
        private final ProjectFramework framework;
        private final String buildTool;
        private final DetectionConfidence confidence;
        private final List<String> evidence;

        public FrameworkDetectionResult(boolean detected, ProjectFramework framework, String buildTool, DetectionConfidence confidence, List<String> evidence) {
            this.detected = detected;
            this.framework = framework;
            this.buildTool = buildTool;
            this.confidence = confidence;
            this.evidence = evidence;
        }

        public boolean isDetected() {
            return detected;
        }

        public ProjectFramework getFramework() {
            return framework;
        }

        public String getBuildTool() {
            return buildTool;
        }

        public DetectionConfidence getConfidence() {
            return confidence;
        }

        public List<String> getEvidence() {
            return evidence;
        }
    }

    FrameworkDetectionResult detect(ProjectSourceReader source);
}
