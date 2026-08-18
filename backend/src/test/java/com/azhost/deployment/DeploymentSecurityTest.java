package com.azhost.deployment;

import com.azhost.deployment.security.DeploymentSecurityPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentSecurityTest {

    @Test
    void isForbiddenFileExtension_ExecutableTypes_ShouldReturnTrue() {
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("script.php")).isTrue();
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("malicious.exe")).isTrue();
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("run.sh")).isTrue();
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("cmd.bat")).isTrue();
    }

    @Test
    void isForbiddenFileExtension_SafeTypes_ShouldReturnFalse() {
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("index.html")).isFalse();
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("style.css")).isFalse();
        assertThat(DeploymentSecurityPolicy.isForbiddenFileExtension("app.js")).isFalse();
    }

    @Test
    void getMimeType_ValidExtensions_ShouldReturnCorrectMimeType() {
        assertThat(DeploymentSecurityPolicy.getMimeType("index.html")).isEqualTo("text/html; charset=utf-8");
        assertThat(DeploymentSecurityPolicy.getMimeType("style.css")).isEqualTo("text/css; charset=utf-8");
        assertThat(DeploymentSecurityPolicy.getMimeType("main.js")).isEqualTo("text/javascript; charset=utf-8");
        assertThat(DeploymentSecurityPolicy.getMimeType("logo.png")).isEqualTo("image/png");
    }
}
