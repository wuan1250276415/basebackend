package com.basebackend.logging.audit.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.*;

class AesEncryptorTest {

    private AesEncryptor encryptor;
    private byte[] validKey;

    @BeforeEach
    void setUp() {
        validKey = new byte[32];
        new SecureRandom().nextBytes(validKey);
        encryptor = new AesEncryptor(validKey);
    }

    // --- Constructor validation ---

    @Test
    void constructor_rejectsNullKey() {
        assertThatThrownBy(() -> new AesEncryptor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsShortKey() {
        assertThatThrownBy(() -> new AesEncryptor(new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsLongKey() {
        assertThatThrownBy(() -> new AesEncryptor(new byte[64]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- String encrypt/decrypt round-trip ---

    @Test
    void encryptDecrypt_roundTrip_ascii() {
        String plaintext = "Hello, audit log!";
        String encrypted = encryptor.encrypt(plaintext);
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(encrypted).isNotEqualTo(plaintext);
    }

    @Test
    void encryptDecrypt_roundTrip_unicode() {
        String plaintext = "审计日志测试 \uD83D\uDD12";
        String decrypted = encryptor.decrypt(encryptor.encrypt(plaintext));
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptDecrypt_roundTrip_emptyString() {
        String decrypted = encryptor.decrypt(encryptor.encrypt(""));
        assertThat(decrypted).isEmpty();
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        assertThat(encryptor.encrypt((String) null)).isNull();
    }

    @Test
    void decrypt_nullInput_returnsNull() {
        assertThat(encryptor.decrypt((String) null)).isNull();
    }

    // --- Byte array encrypt/decrypt round-trip ---

    @Test
    void encryptDecryptBytes_roundTrip() {
        byte[] plaintext = "binary data test".getBytes();
        byte[] encrypted = encryptor.encrypt(plaintext);
        byte[] decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptBytes_nullInput_returnsNull() {
        assertThat(encryptor.encrypt((byte[]) null)).isNull();
    }

    @Test
    void decryptBytes_nullInput_returnsNull() {
        assertThat(encryptor.decrypt((byte[]) null)).isNull();
    }

    @Test
    void decryptBytes_tooShortInput_throwsException() {
        assertThatThrownBy(() -> encryptor.decrypt(new byte[5]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Tamper detection ---

    @Test
    void decrypt_tamperedCiphertext_throwsSecurityException() {
        String encrypted = encryptor.encrypt("sensitive data");
        byte[] cipherBytes = Base64Utils.decode(encrypted);

        // Flip a byte in the ciphertext portion (after the 12-byte IV)
        cipherBytes[15] ^= 0xFF;
        String tampered = Base64Utils.encode(cipherBytes);

        assertThatThrownBy(() -> encryptor.decrypt(tampered))
                .isInstanceOf(SecurityException.class);
    }

    // --- Different keys produce different ciphertext ---

    @Test
    void encrypt_differentKeys_produceDifferentCiphertext() {
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        AesEncryptor otherEncryptor = new AesEncryptor(otherKey);

        String plaintext = "same message";
        String enc1 = encryptor.encrypt(plaintext);
        String enc2 = otherEncryptor.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void decrypt_wrongKey_throwsSecurityException() {
        String encrypted = encryptor.encrypt("secret");

        byte[] wrongKey = new byte[32];
        new SecureRandom().nextBytes(wrongKey);
        AesEncryptor wrongEncryptor = new AesEncryptor(wrongKey);

        assertThatThrownBy(() -> wrongEncryptor.decrypt(encrypted))
                .isInstanceOf(SecurityException.class);
    }

    // --- Same plaintext produces different ciphertexts (random IV) ---

    @Test
    void encrypt_samePlaintext_producesDifferentCiphertext() {
        String plaintext = "determinism check";
        String enc1 = encryptor.encrypt(plaintext);
        String enc2 = encryptor.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);
        // But both decrypt to the same value
        assertThat(encryptor.decrypt(enc1)).isEqualTo(encryptor.decrypt(enc2));
    }

    // --- validateKey ---

    @Test
    void validateKey_validKey_returnsTrue() {
        assertThat(encryptor.validateKey()).isTrue();
    }

    // --- Property: decrypt(encrypt(x)) == x for various inputs ---

    @Test
    void roundTrip_property_variousLengths() {
        for (int len : new int[]{1, 10, 100, 1000, 5000}) {
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append((char) ('a' + (i % 26)));
            }
            String plaintext = sb.toString();
            assertThat(encryptor.decrypt(encryptor.encrypt(plaintext)))
                    .as("Round-trip failed for length %d", len)
                    .isEqualTo(plaintext);
        }
    }
}
