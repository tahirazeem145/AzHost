package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PackageManagerDetector {

    private static final Pattern PKG_MANAGER_PATTERN = Pattern.compile("\"packageManager\"\\s*:\\s*\"(npm|pnpm|yarn|bun)@[^\"]+\"");

    public static class PackageManagerResult {
        private final String packageManager;
        private final DetectionConfidence confidence;
        private final List<String> warnings;
        private final List<String> evidence;

        public PackageManagerResult(String packageManager, DetectionConfidence confidence, List<String> warnings, List<String> evidence) {
            this.packageManager = packageManager;
            this.confidence = confidence;
            this.warnings = warnings;
            this.evidence = evidence;
        }

        public String getPackageManager() {
            return packageManager;
        }

        public DetectionConfidence getConfidence() {
            return confidence;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getEvidence() {
            return evidence;
        }
    }

    public PackageManagerResult detect(ProjectSourceReader source) {
        List<String> evidence = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String declaredPm = null;
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isPresent()) {
            Matcher matcher = PKG_MANAGER_PATTERN.matcher(pkgJsonOpt.get());
            if (matcher.find()) {
                declaredPm = matcher.group(1).toLowerCase();
                evidence.add("package.json declares packageManager: " + declaredPm);
            }
        }

        String lockfilePm = null;
        if (source.fileExists("package-lock.json")) {
            lockfilePm = "npm";
            evidence.add("Detected package-lock.json");
        } else if (source.fileExists("pnpm-lock.yaml")) {
            lockfilePm = "pnpm";
            evidence.add("Detected pnpm-lock.yaml");
        } else if (source.fileExists("yarn.lock")) {
            lockfilePm = "yarn";
            evidence.add("Detected yarn.lock");
        } else if (source.fileExists("bun.lock") || source.fileExists("bun.lockb")) {
            lockfilePm = "bun";
            evidence.add("Detected bun lockfile");
        }

        if (declaredPm != null) {
            if (lockfilePm != null && !declaredPm.equalsIgnoreCase(lockfilePm)) {
                warnings.add("package.json declares " + declaredPm + ", but " + lockfilePm + " lockfile is also present.");
            }
            return new PackageManagerResult(declaredPm.toUpperCase(), DetectionConfidence.HIGH, warnings, evidence);
        }

        if (lockfilePm != null) {
            return new PackageManagerResult(lockfilePm.toUpperCase(), DetectionConfidence.HIGH, warnings, evidence);
        }

        warnings.add("Package manager could not be determined from project metadata.");
        return new PackageManagerResult("UNKNOWN", DetectionConfidence.LOW, warnings, evidence);
    }
}
