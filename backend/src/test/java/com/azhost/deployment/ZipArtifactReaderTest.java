package com.azhost.deployment;

import com.azhost.deployment.artifact.ZipArtifactReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipArtifactReaderTest {

    private ZipArtifactReader artifactReader;

    @BeforeEach
    void setUp() {
        artifactReader = new ZipArtifactReader();
    }

    @Test
    void extractArtifact_ValidZip_ShouldExtractSuccessfully(@TempDir Path tempDir) throws IOException {
        Path zipPath = tempDir.resolve("artifact.zip");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("index.html"));
            zos.write("<html><body>Test</body></html>".getBytes());
            zos.closeEntry();
        }
        Files.write(zipPath, baos.toByteArray());

        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);

        var metadata = artifactReader.extractArtifact(zipPath, workspace);

        assertThat(metadata.hasIndexHtml()).isTrue();
        assertThat(metadata.getTotalFileCount()).isEqualTo(1);
        assertThat(workspace.resolve("index.html")).exists();
    }

    @Test
    void extractArtifact_ZipSlipAttempt_ShouldThrowSecurityException(@TempDir Path tempDir) throws IOException {
        Path zipPath = tempDir.resolve("malicious.zip");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("../malicious.txt"));
            zos.write("payload".getBytes());
            zos.closeEntry();
        }
        Files.write(zipPath, baos.toByteArray());

        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);

        assertThatThrownBy(() -> artifactReader.extractArtifact(zipPath, workspace))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void extractArtifact_ForbiddenExecutableFile_ShouldThrowSecurityException(@TempDir Path tempDir) throws IOException {
        Path zipPath = tempDir.resolve("evil.zip");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("script.php"));
            zos.write("<?php echo 'evil'; ?>".getBytes());
            zos.closeEntry();
        }
        Files.write(zipPath, baos.toByteArray());

        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);

        assertThatThrownBy(() -> artifactReader.extractArtifact(zipPath, workspace))
                .isInstanceOf(SecurityException.class);
    }
}
