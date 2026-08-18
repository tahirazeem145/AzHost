package com.azhost.analysis;

import com.azhost.analysis.detector.NodeVersionDetector;
import com.azhost.analysis.source.LocalDirectorySourceReader;
import com.azhost.analysis.source.ProjectSourceReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class NodeVersionDetectorTest {

    private final NodeVersionDetector detector = new NodeVersionDetector();
    private final Path fixturesRoot = Paths.get("src/test/resources/test-fixtures").toAbsolutePath().normalize();

    @Test
    void detect_NodeVersionConflict_ShouldReturnEnginesVersionAndWarning() {
        ProjectSourceReader source = new LocalDirectorySourceReader(fixturesRoot.resolve("node-conflict"));
        NodeVersionDetector.NodeVersionResult result = detector.detect(source);

        assertThat(result.getNodeVersion()).isEqualTo(">=18");
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("Conflicting Node.js versions declared across package.json");
    }
}
