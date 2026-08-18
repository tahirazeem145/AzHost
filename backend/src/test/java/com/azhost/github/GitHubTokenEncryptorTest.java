package com.azhost.github;

import com.azhost.github.security.GitHubTokenEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubTokenEncryptorTest {

    private GitHubTokenEncryptor encryptor;
    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        encryptor = new GitHubTokenEncryptor(TEST_KEY);
    }

    @Test
    void shouldEncryptAndDecryptTokenSuccessfully() {
        String token = "gho_1234567890abcdefghijklmnopqrstuvwxyz";
        String encrypted = encryptor.encrypt(token);

        assertNotNull(encrypted);
        assertNotEquals(token, encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(token, decrypted);
    }

    @Test
    void shouldGenerateUniqueIVPerEncryption() {
        String token = "gho_same_token_value";
        String encrypted1 = encryptor.encrypt(token);
        String encrypted2 = encryptor.encrypt(token);

        assertNotEquals(encrypted1, encrypted2, "Encrypted strings should differ due to random IV");
        assertEquals(token, encryptor.decrypt(encrypted1));
        assertEquals(token, encryptor.decrypt(encrypted2));
    }

    @Test
    void shouldThrowExceptionWhenKeyIsMissing() {
        assertThrows(IllegalStateException.class, () -> new GitHubTokenEncryptor(null));
        assertThrows(IllegalStateException.class, () -> new GitHubTokenEncryptor(""));
    }

    @Test
    void shouldThrowExceptionOnTamperedCiphertext() {
        String token = "gho_secret_token";
        String encrypted = encryptor.encrypt(token);
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tampered));
    }
}
