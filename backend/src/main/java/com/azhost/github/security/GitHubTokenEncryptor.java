package com.azhost.github.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class GitHubTokenEncryptor {

    private static final Logger logger = LoggerFactory.getLogger(GitHubTokenEncryptor.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public GitHubTokenEncryptor(@Value("${azhost.security.github-token-encryption-key:}") String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: azhost.security.github-token-encryption-key. Encryption key must be provided.");
        }

        byte[] keyBytes;
        if (rawKey.length() == 64 && isHex(rawKey)) {
            keyBytes = HexFormat.of().parseHex(rawKey);
        } else {
            try {
                keyBytes = Base64.getDecoder().decode(rawKey);
            } catch (IllegalArgumentException e) {
                keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
            }
        }

        if (keyBytes.length != 32) {
            // Pad or hash key if non-standard length to guarantee 256-bit AES key
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(keyBytes);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize AES-256 encryption key", e);
            }
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintextToken) {
        if (plaintextToken == null || plaintextToken.isBlank()) {
            throw new IllegalArgumentException("Token to encrypt cannot be null or blank");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintextToken.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            logger.error("Token encryption failed safely");
            throw new IllegalStateException("Failed to encrypt OAuth token", e);
        }
    }

    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalArgumentException("Encrypted token cannot be null or blank");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedToken);

            if (decoded.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted token format");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Token decryption failed safely");
            throw new IllegalStateException("Failed to decrypt OAuth token", e);
        }
    }

    private static boolean isHex(String s) {
        return s.matches("^[0-9a-fA-F]+$");
    }
}
