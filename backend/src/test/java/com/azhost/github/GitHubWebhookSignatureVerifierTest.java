package com.azhost.github;

import com.azhost.github.security.GitHubWebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GitHubWebhookSignatureVerifierTest {

    private GitHubWebhookSignatureVerifier verifier;

    private static final String TEST_SECRET = "test-webhook-secret-12345";
    private static final String TEST_PAYLOAD = "{\"action\":\"push\",\"ref\":\"refs/heads/main\"}";

    @BeforeEach
    void setUp() {
        verifier = new GitHubWebhookSignatureVerifier();
    }

    @Test
    void shouldVerifyValidSignature() throws Exception {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacSha256Hex(TEST_SECRET, payload);

        assertTrue(verifier.verify(signature, payload, TEST_SECRET),
                "Valid HMAC-SHA256 signature should pass verification");
    }

    @Test
    void shouldRejectTamperedPayload() throws Exception {
        byte[] originalPayload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacSha256Hex(TEST_SECRET, originalPayload);

        byte[] tamperedPayload = "{\"action\":\"malicious\"}".getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(signature, tamperedPayload, TEST_SECRET),
                "Tampered payload should not match original signature");
    }

    @Test
    void shouldRejectMissingSignatureHeader() {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify(null, payload, TEST_SECRET));
        assertFalse(verifier.verify("", payload, TEST_SECRET));
        assertFalse(verifier.verify("   ", payload, TEST_SECRET));
    }

    @Test
    void shouldRejectSignatureWithoutPrefix() throws Exception {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String hexOnly = computeHmacSha256Hex(TEST_SECRET, payload);

        // No "sha256=" prefix
        assertFalse(verifier.verify(hexOnly, payload, TEST_SECRET),
                "Signature without 'sha256=' prefix should be rejected");
    }

    @Test
    void shouldRejectMissingOrBlankSecret() {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=anyvalue";

        assertFalse(verifier.verify(signature, payload, null));
        assertFalse(verifier.verify(signature, payload, ""));
        assertFalse(verifier.verify(signature, payload, "   "));
    }

    @Test
    void shouldRejectEmptyPayload() throws Exception {
        String signature = "sha256=" + computeHmacSha256Hex(TEST_SECRET, new byte[0]);

        assertFalse(verifier.verify(signature, null, TEST_SECRET));
        assertFalse(verifier.verify(signature, new byte[0], TEST_SECRET));
    }

    @Test
    void shouldRejectInvalidHexInSignature() {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertFalse(verifier.verify("sha256=ZZZNOTVALIDHEX", payload, TEST_SECRET),
                "Invalid hex in signature should be rejected gracefully");
    }

    @Test
    void shouldRejectWrongSecret() throws Exception {
        byte[] payload = TEST_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacSha256Hex("wrong-secret", payload);

        assertFalse(verifier.verify(signature, payload, TEST_SECRET),
                "Signature computed with wrong secret should fail verification");
    }

    @Test
    void shouldHandleUnicodePayload() throws Exception {
        byte[] payload = "Push to репозиторий".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmacSha256Hex(TEST_SECRET, payload);

        assertTrue(verifier.verify(signature, payload, TEST_SECRET),
                "Should correctly verify non-ASCII payload bytes");
    }

    // Helper: compute HMAC-SHA256 for test setup
    private static String computeHmacSha256Hex(String secret, byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
