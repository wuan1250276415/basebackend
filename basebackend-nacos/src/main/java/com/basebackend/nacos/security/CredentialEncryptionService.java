/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.basebackend.nacos.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class CredentialEncryptionService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionService.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    public static final String ENCRYPTED_PREFIX = "ENC(";
    public static final String ENCRYPTED_SUFFIX = ")";
    private static final String DEFAULT_KEY_SEED = "basebackend-nacos-encryption-key";
    private static final String KEY_ENV_NAME = "NACOS_ENCRYPTION_KEY";
    private static final String KEY_PROPERTY_NAME = "nacos.encryption.key";
    private static final String FAIL_ON_DEFAULT_ENV_NAME = "NACOS_ENCRYPTION_FAIL_ON_DEFAULT_KEY";
    private static final String FAIL_ON_DEFAULT_PROPERTY_NAME = "nacos.encryption.fail-on-default-key";
    private static final String ACTIVE_PROFILE_ENV_NAME = "SPRING_PROFILES_ACTIVE";
    private static final String ACTIVE_PROFILE_PROPERTY_NAME = "spring.profiles.active";
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public CredentialEncryptionService() {
        this(null);
    }

    public CredentialEncryptionService(Environment environment) {
        String keySeed = CredentialEncryptionService.resolveKeySeed(environment);
        boolean useDefaultKeySeed = DEFAULT_KEY_SEED.equals(keySeed);
        if (useDefaultKeySeed) {
            if (CredentialEncryptionService.failOnDefaultKey(environment)) {
                throw new IllegalStateException("Default nacos encryption key is forbidden in current environment. Please set NACOS_ENCRYPTION_KEY or -Dnacos.encryption.key.");
            }
            log.warn("Using default encryption key seed. Set NACOS_ENCRYPTION_KEY environment variable for production!");
        }
        this.secretKey = this.deriveKey(keySeed);
        this.secureRandom = new SecureRandom();
        log.info("CredentialEncryptionService initialized");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        if (this.isEncrypted(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[12];
            this.secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(128, iv);
            cipher.init(1, (Key)this.secretKey, paramSpec);
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[12 + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, 12);
            System.arraycopy(cipherBytes, 0, combined, 12, cipherBytes.length);
            String encrypted = Base64.getEncoder().encodeToString(combined);
            return ENCRYPTED_PREFIX + encrypted + ENCRYPTED_SUFFIX;
        }
        catch (Exception e) {
            log.error("Encryption failed", (Throwable)e);
            throw new RuntimeException("\u52a0\u5bc6\u5931\u8d25", e);
        }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!this.isEncrypted(cipherText)) {
            return cipherText;
        }
        try {
            String encrypted = cipherText.substring(ENCRYPTED_PREFIX.length(), cipherText.length() - ENCRYPTED_SUFFIX.length());
            byte[] combined = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[12];
            byte[] cipherBytes = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec paramSpec = new GCMParameterSpec(128, iv);
            cipher.init(2, (Key)this.secretKey, paramSpec);
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            log.error("Decryption failed", (Throwable)e);
            throw new RuntimeException("\u89e3\u5bc6\u5931\u8d25", e);
        }
    }

    public boolean isEncrypted(String text) {
        return text != null && text.startsWith(ENCRYPTED_PREFIX) && text.endsWith(ENCRYPTED_SUFFIX);
    }

    public String decryptIfNeeded(String value) {
        if (this.isEncrypted(value)) {
            return this.decrypt(value);
        }
        return value;
    }

    private SecretKey deriveKey(String keySeed) {
        try {
            byte[] salt = "nacos-credential-salt".getBytes(StandardCharsets.UTF_8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(keySeed.toCharArray(), salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    public static String resolveKeySeed() {
        return CredentialEncryptionService.resolveKeySeed(null);
    }

    public static String resolveKeySeed(Environment environment) {
        String keySeed = CredentialEncryptionService.firstNonBlank(CredentialEncryptionService.readProperty(environment, KEY_PROPERTY_NAME), System.getProperty(KEY_PROPERTY_NAME), System.getenv(KEY_ENV_NAME));
        if (CredentialEncryptionService.hasText(keySeed)) {
            return keySeed.trim();
        }
        return DEFAULT_KEY_SEED;
    }

    public static boolean failOnDefaultKey() {
        return CredentialEncryptionService.failOnDefaultKey(null);
    }

    public static boolean failOnDefaultKey(Environment environment) {
        String configured = CredentialEncryptionService.firstNonBlank(CredentialEncryptionService.readProperty(environment, FAIL_ON_DEFAULT_PROPERTY_NAME), System.getProperty(FAIL_ON_DEFAULT_PROPERTY_NAME), System.getenv(FAIL_ON_DEFAULT_ENV_NAME));
        if (CredentialEncryptionService.hasText(configured)) {
            return Boolean.parseBoolean(configured.trim());
        }
        return CredentialEncryptionService.isProductionProfileActive(environment);
    }

    public static boolean isDefaultKeySeedInUse() {
        return CredentialEncryptionService.isDefaultKeySeedInUse(null);
    }

    public static boolean isDefaultKeySeedInUse(Environment environment) {
        return DEFAULT_KEY_SEED.equals(CredentialEncryptionService.resolveKeySeed(environment));
    }

    static boolean isProductionProfileActive() {
        return CredentialEncryptionService.isProductionProfileActive(null);
    }

    static boolean isProductionProfileActive(Environment environment) {
        if (environment != null) {
            String[] activeProfiles = environment.getActiveProfiles();
            if (CredentialEncryptionService.containsProductionProfile(activeProfiles)) {
                return true;
            }
            String configuredProfiles = environment.getProperty(ACTIVE_PROFILE_PROPERTY_NAME);
            if (CredentialEncryptionService.hasText(configuredProfiles) && CredentialEncryptionService.containsProductionProfile(configuredProfiles.split(","))) {
                return true;
            }
        }
        String configuredProfiles = CredentialEncryptionService.firstNonBlank(System.getProperty(ACTIVE_PROFILE_PROPERTY_NAME), System.getenv(ACTIVE_PROFILE_ENV_NAME));
        if (!CredentialEncryptionService.hasText(configuredProfiles)) {
            return false;
        }
        return CredentialEncryptionService.containsProductionProfile(configuredProfiles.split(","));
    }

    private static boolean containsProductionProfile(String[] profiles) {
        if (profiles == null || profiles.length == 0) {
            return false;
        }
        for (String profile : profiles) {
            String normalized = profile.trim().toLowerCase();
            if ("prod".equals(normalized) || "production".equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String ... candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        for (String candidate : candidates) {
            if (!CredentialEncryptionService.hasText(candidate)) continue;
            return candidate;
        }
        return null;
    }

    private static String readProperty(Environment environment, String key) {
        if (environment == null) {
            return null;
        }
        return environment.getProperty(key);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
