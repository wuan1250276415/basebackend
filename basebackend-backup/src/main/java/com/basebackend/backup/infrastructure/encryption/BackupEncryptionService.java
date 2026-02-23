package com.basebackend.backup.infrastructure.encryption;

import com.basebackend.backup.config.BackupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Slf4j
@RequiredArgsConstructor
public class BackupEncryptionService {

    private static final byte[] MAGIC = {'B', 'B', 'E', 'N'};
    private static final int BUFFER_SIZE = 8192;

    private final BackupProperties backupProperties;

    public File encrypt(File input) throws Exception {
        BackupProperties.Encryption config = backupProperties.getEncryption();
        SecretKey key = loadKey(config.getLocalKey());

        byte[] iv = new byte[config.getIvLengthBytes()];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(config.getTagLengthBits(), iv));

        File encryptedFile = new File(input.getAbsolutePath() + ".enc");

        try (InputStream fis = new BufferedInputStream(new FileInputStream(input));
             OutputStream fos = new BufferedOutputStream(new FileOutputStream(encryptedFile))) {

            // 写入文件头: MAGIC(4) + IV长度(4) + IV(N)
            fos.write(MAGIC);
            fos.write(ByteBuffer.allocate(4).putInt(iv.length).array());
            fos.write(iv);

            // 加密写入
            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, bytesRead);
                }
            }
        }

        log.info("备份文件加密完成: {} -> {}, 原始大小: {} bytes",
                input.getName(), encryptedFile.getName(), input.length());
        return encryptedFile;
    }

    public File decrypt(File input) throws Exception {
        BackupProperties.Encryption config = backupProperties.getEncryption();
        SecretKey key = loadKey(config.getLocalKey());

        try (InputStream fis = new BufferedInputStream(new FileInputStream(input))) {
            // 读取并验证 MAGIC
            byte[] magic = new byte[4];
            if (fis.read(magic) != 4 || !Arrays.equals(magic, MAGIC)) {
                throw new IllegalArgumentException("文件不是加密备份文件: " + input.getName());
            }

            // 读取 IV 长度和 IV
            byte[] ivLenBytes = new byte[4];
            if (fis.read(ivLenBytes) != 4) {
                throw new IllegalArgumentException("无法读取IV长度");
            }
            int ivLen = ByteBuffer.wrap(ivLenBytes).getInt();
            byte[] iv = new byte[ivLen];
            if (fis.read(iv) != ivLen) {
                throw new IllegalArgumentException("无法读取IV");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(config.getTagLengthBits(), iv));

            // 解密到临时文件
            String decryptedPath = input.getAbsolutePath().replace(".enc", "");
            File decryptedFile = new File(decryptedPath);

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 OutputStream fos = new BufferedOutputStream(new FileOutputStream(decryptedFile))) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            log.info("备份文件解密完成: {} -> {}", input.getName(), decryptedFile.getName());
            return decryptedFile;
        }
    }

    public boolean isEncryptedFile(File file) {
        if (file == null || !file.exists() || file.length() < 8) {
            return false;
        }
        try (InputStream fis = new FileInputStream(file)) {
            byte[] magic = new byte[4];
            return fis.read(magic) == 4 && Arrays.equals(magic, MAGIC);
        } catch (IOException e) {
            return false;
        }
    }

    private SecretKey loadKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("备份加密密钥未配置 (backup.encryption.local-key)");
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256密钥长度必须为32字节, 当前: " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
