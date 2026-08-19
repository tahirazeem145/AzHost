package com.azhost.github.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies GitHub webhook payload signatures using HMAC-SHA256.
 *
 * GitHub sends a SHA256 HMAC signature in the X-Hub-Signature-256 header
 * in the format: "sha256=<hex-digest>"
 *
 * Security properties:
 * - Uses MessageDigest.isEqual() for constant-time comparison (prevents timing attacks)
 * - Never logs the webhook secret or computed HMAC
 * - Rejects missing or malformed signatures immediately
 */
@Component
public class GitHubWebhookSignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    /**
     * Verify a webhook signature against the request payload.
     *
     * @param signatureHeader the value of X-Hub-Signature-256 (e.g. "sha256=abc123...")
     * @param rawPayload      the raw request body bytes (must be the original, unmodified bytes)
     * @param webhookSecret   the per-project plaintext webhook secret to validate against
     * @return true if the signature is valid, false otherwise
     */
    public boolean verify(String signatureHeader, byte[] rawPayload, String webhookSecret) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            logger.warn("Webhook signature header is missing");
            return false;
        }

        if (!signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            logger.warn("Webhook signature header has unexpected format (expected 'sha256=' prefix)");
            return false;
        }

        if (webhookSecret == null || webhookSecret.isBlank()) {
            logger.warn("Webhook secret is not configured for this project");
            return false;
        }

        if (rawPayload == null || rawPayload.length == 0) {
            logger.warn("Webhook payload is empty");
            return false;
        }

        try {
            String expectedHex = signatureHeader.substring(SIGNATURE_PREFIX.length());
            byte[] expectedBytes = hexToBytes(expectedHex);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] actualBytes = mac.doFinal(rawPayload);

            // Constant-time comparison prevents timing side-channel attacks
            boolean valid = MessageDigest.isEqual(expectedBytes, actualBytes);
            if (!valid) {
                logger.warn("Webhook signature verification failed — payload may have been tampered with");
            }
            return valid;

        } catch (NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            logger.warn("Webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convert a hex string to bytes.
     * Throws IllegalArgumentException for invalid hex strings.
     */
    private static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string length");
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(hex.charAt(i), 16);
            int low  = Character.digit(hex.charAt(i + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character in signature");
            }
            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }
}
