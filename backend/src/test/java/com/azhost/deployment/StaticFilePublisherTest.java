package com.azhost.deployment;

import com.azhost.deployment.publisher.LocalStaticFilePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticFilePublisherTest {

    private LocalStaticFilePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new LocalStaticFilePublisher();
    }

    @Test
    void publishStaticSite_ValidDirectoryWithIndexHtml_ShouldCopyFilesSuccessfully(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("extracted");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("index.html"), "<h1>Published</h1>");

        Path targetDir = tempDir.resolve("deployments/dep-1");

        Path result = publisher.publishStaticSite(sourceDir, targetDir);

        assertThat(result).isEqualTo(targetDir);
        assertThat(targetDir.resolve("index.html")).exists();
    }

    @Test
    void publishStaticSite_MissingIndexHtml_ShouldThrowIllegalArgumentException(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("extracted-no-index");
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("app.js"), "console.log('hi');");

        Path targetDir = tempDir.resolve("deployments/dep-2");

        assertThatThrownBy(() -> publisher.publishStaticSite(sourceDir, targetDir))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
