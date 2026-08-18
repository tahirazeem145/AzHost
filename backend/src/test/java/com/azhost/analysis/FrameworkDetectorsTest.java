package com.azhost.analysis;

import com.azhost.analysis.detector.*;
import com.azhost.analysis.source.LocalDirectorySourceReader;
import com.azhost.analysis.source.ProjectSourceReader;
import com.azhost.entity.ProjectFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class FrameworkDetectorsTest {

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
    void detect_ReactViteProject_ShouldReturnReactAndVite() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("react-vite"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.REACT);
        assertThat(result.getBuildTool()).isEqualTo("Vite");
        assertThat(result.getOutputDirectory()).isEqualTo("dist");
        assertThat(result.getLanguage()).isEqualTo("TYPESCRIPT");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
        assertThat(result.isExecuted()).isFalse();
    }

    @Test
    void detect_ReactCraProject_ShouldReturnReactAndCra() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("react-cra"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.REACT);
        assertThat(result.getBuildTool()).isEqualTo("Create React App");
        assertThat(result.getOutputDirectory()).isEqualTo("build");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
    }

    @Test
    void detect_NextJsProject_ShouldReturnNextJs() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("nextjs"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.NEXT_JS);
        assertThat(result.getBuildTool()).isEqualTo("Next.js");
        assertThat(result.getOutputDirectory()).isEqualTo(".next");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
    }

    @Test
    void detect_VueViteProject_ShouldReturnVueAndVite() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("vue-vite"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.VUE);
        assertThat(result.getBuildTool()).isEqualTo("Vite");
        assertThat(result.getOutputDirectory()).isEqualTo("dist");
    }

    @Test
    void detect_AngularProject_ShouldReturnAngular() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("angular"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.ANGULAR);
        assertThat(result.getBuildTool()).isEqualTo("Angular CLI");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
    }

    @Test
    void detect_StaticProject_ShouldReturnStaticAndDotOutput() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("static"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.STATIC);
        assertThat(result.getOutputDirectory()).isEqualTo(".");
        assertThat(result.getOutputDirectory()).isNotEqualTo("/");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
    }

    @Test
    void detect_UnknownProject_ShouldReturnUnknownAndLowConfidence() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("unknown"));
        ProjectAnalysisResult result = projectAnalyzer.analyze(source);

        assertThat(result.getFramework()).isEqualTo(ProjectFramework.UNKNOWN);
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.LOW);
    }
}
