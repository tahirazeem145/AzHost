package com.azhost.analysis.detector;

import com.azhost.analysis.DetectionConfidence;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class NextJsDetector implements FrameworkDetector {

    @Override
    public FrameworkDetectionResult detect(ProjectSourceReader source) {
        Optional<String> pkgJsonOpt = source.readFileContent("package.json");
        if (pkgJsonOpt.isEmpty()) {
            return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
        }

        String pkgJson = pkgJsonOpt.get();
        List<String> evidence = new ArrayList<>();

        boolean hasNextDep = pkgJson.contains("\"next\"");
        boolean hasNextScripts = pkgJson.contains("next build") || pkgJson.contains("next dev") || pkgJson.contains("next start");

        if (hasNextDep) {
            evidence.add("package.json contains 'next' dependency");
        }
        if (hasNextScripts) {
            evidence.add("package.json contains Next.js build/dev scripts");
        }

        boolean hasNextConfig = source.fileExists("next.config.js") || source.fileExists("next.config.ts") || source.fileExists("next.config.mjs");
        if (hasNextConfig) {
            evidence.add("Next.js configuration file exists");
        }

        if (hasNextDep || hasNextScripts) {
            return new FrameworkDetectionResult(true, ProjectFramework.NEXT_JS, "Next.js", DetectionConfidence.HIGH, evidence);
        }

        return new FrameworkDetectionResult(false, ProjectFramework.UNKNOWN, null, DetectionConfidence.LOW, List.of());
    }
}
