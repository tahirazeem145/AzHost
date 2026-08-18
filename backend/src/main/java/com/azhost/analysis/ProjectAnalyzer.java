package com.azhost.analysis;

import com.azhost.analysis.detector.*;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProjectAnalyzer {

    private final List<FrameworkDetector> frameworkDetectors;
    private final PackageManagerDetector packageManagerDetector;
    private final NodeVersionDetector nodeVersionDetector;
    private final BuildCommandDetector buildCommandDetector;
    private final OutputDirectoryDetector outputDirectoryDetector;

    public ProjectAnalyzer(
            NextJsDetector nextJsDetector,
            AngularDetector angularDetector,
            ReactViteDetector reactViteDetector,
            ReactCraDetector reactCraDetector,
            VueViteDetector vueViteDetector,
            StaticSiteDetector staticSiteDetector,
            UnknownFrameworkDetector unknownFrameworkDetector,
            PackageManagerDetector packageManagerDetector,
            NodeVersionDetector nodeVersionDetector,
            BuildCommandDetector buildCommandDetector,
            OutputDirectoryDetector outputDirectoryDetector
    ) {
        // Deterministic detection order
        this.frameworkDetectors = List.of(
                nextJsDetector,
                angularDetector,
                reactViteDetector,
                reactCraDetector,
                vueViteDetector,
                staticSiteDetector,
                unknownFrameworkDetector
        );
        this.packageManagerDetector = packageManagerDetector;
        this.nodeVersionDetector = nodeVersionDetector;
        this.buildCommandDetector = buildCommandDetector;
        this.outputDirectoryDetector = outputDirectoryDetector;
    }

    public ProjectAnalysisResult analyze(ProjectSourceReader source) {
        ProjectAnalysisResult result = new ProjectAnalysisResult();
        result.setAnalyzedAt(ZonedDateTime.now());
        result.setExecuted(false);

        List<String> evidence = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> detectedFiles = source.listRootFiles();
        result.setDetectedFiles(detectedFiles);

        // 1. Framework Detection
        FrameworkDetector.FrameworkDetectionResult frameworkResult = null;
        for (FrameworkDetector detector : frameworkDetectors) {
            FrameworkDetector.FrameworkDetectionResult res = detector.detect(source);
            if (res.isDetected()) {
                frameworkResult = res;
                break;
            }
        }

        if (frameworkResult != null) {
            result.setFramework(frameworkResult.getFramework());
            result.setFrameworkConfidence(frameworkResult.getConfidence());
            result.setBuildTool(frameworkResult.getBuildTool());
            evidence.addAll(frameworkResult.getEvidence());
        } else {
            result.setFramework(ProjectFramework.UNKNOWN);
            result.setFrameworkConfidence(DetectionConfidence.LOW);
            result.setBuildTool("Unknown");
        }

        // 2. Package Manager Detection
        PackageManagerDetector.PackageManagerResult pmResult = packageManagerDetector.detect(source);
        result.setPackageManager(pmResult.getPackageManager());
        result.setPackageManagerConfidence(pmResult.getConfidence());
        evidence.addAll(pmResult.getEvidence());
        warnings.addAll(pmResult.getWarnings());

        // 3. Node Version Detection
        NodeVersionDetector.NodeVersionResult nodeResult = nodeVersionDetector.detect(source);
        result.setNodeVersion(nodeResult.getNodeVersion());
        evidence.addAll(nodeResult.getEvidence());
        warnings.addAll(nodeResult.getWarnings());

        // 4. Build / Dev Commands
        BuildCommandDetector.CommandResult cmdResult = buildCommandDetector.detect(source, result.getPackageManager());
        result.setBuildCommand(cmdResult.getBuildCommand());
        result.setDevCommand(cmdResult.getDevCommand());

        // 5. Output Directory
        String outputDir = outputDirectoryDetector.detectOutputDirectory(result.getFramework(), result.getBuildTool());
        result.setOutputDirectory(outputDir);

        // 6. Language Detection
        boolean isTypeScript = source.fileExists("tsconfig.json") ||
                source.fileExists("vite.config.ts") ||
                source.fileExists("next.config.ts");
        result.setLanguage(isTypeScript ? "TYPESCRIPT" : (result.getFramework() == ProjectFramework.STATIC ? "HTML" : "JAVASCRIPT"));
        if (isTypeScript) {
            evidence.add("TypeScript indicators detected");
        }

        // Overall Confidence calculation
        DetectionConfidence overallConfidence = result.getFrameworkConfidence();
        if (result.getFramework() == ProjectFramework.UNKNOWN) {
            overallConfidence = DetectionConfidence.LOW;
            warnings.add("Unable to determine a supported framework.");
        }
        result.setConfidence(overallConfidence);

        result.setEvidence(evidence);
        result.setWarnings(warnings);

        return result;
    }
}
