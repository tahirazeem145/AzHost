package com.azhost.analysis;

import com.azhost.analysis.detector.PackageManagerDetector;
import com.azhost.analysis.source.LocalDirectorySourceReader;
import com.azhost.analysis.source.ProjectSourceReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class PackageManagerDetectorTest {

    private final PackageManagerDetector detector = new PackageManagerDetector();
    private final Path fixturesRoot = Paths.get("src/test/resources/test-fixtures").toAbsolutePath().normalize();

    @Test
    void detect_PnpmWithLockfileConflict_ShouldReturnPnpmAndWarning() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("pnpm-conflict"));
        PackageManagerDetector.PackageManagerResult result = detector.detect(source);

        assertThat(result.getPackageManager()).isEqualTo("PNPM");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.HIGH);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("package.json declares pnpm, but npm lockfile is also present");
    }

    @Test
    void detect_UnknownPackageManager_ShouldReturnUnknownAndLowConfidence() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("unknown"));
        PackageManagerDetector.PackageManagerResult result = detector.detect(source);

        assertThat(result.getPackageManager()).isEqualTo("UNKNOWN");
        assertThat(result.getConfidence()).isEqualTo(DetectionConfidence.LOW);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("Package manager could not be determined");
    }
}
