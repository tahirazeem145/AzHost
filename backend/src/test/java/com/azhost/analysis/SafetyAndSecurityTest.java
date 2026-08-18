package com.azhost.analysis;

import com.azhost.analysis.detector.*;
import com.azhost.analysis.source.LocalDirectorySourceReader;
import com.azhost.analysis.source.ProjectSourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyAndSecurityTest {

    private ProjectAnalyzer projectAnalyzer;
    private final Path fixturesRoot = Paths.get("src/test/resources/test-fixtures").toAbsolutePath().normalize();

    @BeforeEach
    void setUp() {
        projectAnalyzer = new ProjectAnalyzer(
                new NextJsDetector(),
                new AngularDetector(),
                new ReactViteDetector(),
                new ReactCraDetector(),
                new VueViteDetector(),
                new StaticSiteDetector(),
                new UnknownFrameworkDetector(),
                new PackageManagerDetector(),
                new NodeVersionDetector(),
                new BuildCommandDetector(),
                new OutputDirectoryDetector()
        );
    }

    @Test
    void pathTraversal_ShouldBeRejectedBySourceReader() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("react-vite"));

        Optional<String> content = source.readFileContent("../react-cra/package.json");
        assertThat(content).isEmpty();

        Optional<String> contentAbs = source.readFileContent("C:/Windows/System32/drivers/etc/hosts");
        assertThat(contentAbs).isEmpty();
    }

    @Test
    void maliciousPackageJsonScript_ShouldNeverBeExecuted() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("malicious-script"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getBuildCommand()).isEqualTo("npm run build");
        assertThat(result.isExecuted()).isFalse();
    }

}
