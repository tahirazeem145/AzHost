package com.azhost.analysis.detector;

import com.azhost.analysis.source.ProjectSourceReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NodeVersionDetector {

    private static final Pattern NODE_ENGINE_PATTERN = Pattern.compile("\"node\"\\s*:\\s*\"([^\"]+)\"");

    public static class NodeVersionResult {
        private final String nodeVersion;
        private final List<String> warnings;
        private final List<String> evidence;

        public NodeVersionResult(String nodeVersion, List<String> warnings, List<String> evidence) {
            this.nodeVersion = nodeVersion;
            this.warnings = warnings;
            this.evidence = evidence;
        }

        public String getNodeVersion() {
            return nodeVersion;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getEvidence() {
            return evidence;
        }
    }

    public NodeVersionResult detect(ProjectSourceReader source) {
        List<String> evidence = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String engineVersion = null;
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isPresent()) {
            Matcher matcher = NODE_ENGINE_PATTERN.matcher(pkgJsonOpt.get());
            if (matcher.find()) {
                engineVersion = matcher.group(1).trim();
                evidence.add("package.json engines.node: " + engineVersion);
            }
        }

        String nvmrcVersion = null;
        Optional<String> nvmrcOpt = source.readFileContent(".nvmrc");
        if (nvmrcOpt.isPresent()) {
            nvmrcVersion = nvmrcOpt.get().trim();
            evidence.add(".nvmrc declares Node version: " + nvmrcVersion);
        }

        String nodeVersionFile = null;
        Optional<String> nodeVerOpt = source.readFileContent(".node-version");
        if (nodeVerOpt.isPresent()) {
            nodeVersionFile = nodeVerOpt.get().trim();
            evidence.add(".node-version declares Node version: " + nodeVersionFile);
        }

        // Conflict checking
        if (engineVersion != null && nvmrcVersion != null && !engineVersion.equals(nvmrcVersion)) {
            warnings.add("Conflicting Node.js versions declared across package.json (" + engineVersion + ") and .nvmrc (" + nvmrcVersion + ").");
        }

        if (engineVersion != null) {
            return new NodeVersionResult(engineVersion, warnings, evidence);
        }

        if (nvmrcVersion != null) {
            return new NodeVersionResult(nvmrcVersion, warnings, evidence);
        }

        if (nodeVersionFile != null) {
            return new NodeVersionResult(nodeVersionFile, warnings, evidence);
        }

        return new NodeVersionResult("Not detected", warnings, evidence);
    }
}
