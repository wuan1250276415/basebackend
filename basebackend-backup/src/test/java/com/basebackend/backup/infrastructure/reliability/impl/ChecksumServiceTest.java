package com.basebackend.backup.infrastructure.reliability.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.reliability.Checksum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 校验服务测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChecksumService 校验服务测试")
class ChecksumServiceTest {

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.Checksum checksumConfig;

    @TempDir
    Path tempDir;

    private ChecksumService checksumService;

    @BeforeEach
    void setUp() {
        checksumService = new ChecksumService(backupProperties);
        when(backupProperties.getChecksum()).thenReturn(checksumConfig);
        // 注意：不设置默认的algorithms，让每个测试自己设置
    }

    @Test
    @DisplayName("计算空文件的MD5校验和")
    void shouldComputeMd5ForEmptyFile() throws Exception {
        // Given
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        Checksum checksum = checksumService.computeChecksum(emptyFile);

        // Then
        assertThat(checksum.getMd5()).isEqualTo("d41d8cd98f00b204e9800998ecf8427e"); // MD5 of empty file
        assertThat(checksum.getSha256()).isNull();
        assertThat(checksum.getFileSize()).isEqualTo(0L);
    }

    @Test
    @DisplayName("计算非空文件的MD5校验和")
    void shouldComputeMd5ForNonEmptyFile() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        Checksum checksum = checksumService.computeChecksum(testFile);

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getSha256()).isNull();
        assertThat(checksum.getFileSize()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("同时计算MD5和SHA256")
    void shouldComputeBothMd5AndSha256() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5", "SHA256"));

        // When
        Checksum checksum = checksumService.computeChecksum(testFile);

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getSha256()).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
        assertThat(checksum.getFileSize()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("文件不存在时应该抛出异常")
    void shouldThrowExceptionWhenFileNotFound() {
        // Given
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When & Then
        assertThatThrownBy(() -> checksumService.computeChecksum(nonExistentFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("文件不存在");
    }

    @Test
    @DisplayName("不支持的算法应该被跳过并记录警告")
    void shouldSkipUnsupportedAlgorithm() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5", "SHA512", "UNSUPPORTED"));

        // When
        Checksum checksum = checksumService.computeChecksum(testFile);

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getSha256()).isNull(); // SHA256 not in list
        assertThat(checksum.getAlgorithms()).containsExactly("MD5", "SHA512", "UNSUPPORTED");
    }

    @Test
    @DisplayName("大小写不敏感的算法处理")
    void shouldHandleCaseInsensitiveAlgorithms() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("md5", "sha256", "Sha512"));

        // When
        Checksum checksum = checksumService.computeChecksum(testFile);

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getSha256()).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
    }

    @Test
    @DisplayName("计算输入流的MD5校验和")
    void shouldComputeMd5ForInputStream() throws Exception {
        // Given
        String content = "Hello, World!";
        InputStream inputStream = createInputStream(content);
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        Checksum checksum = checksumService.computeChecksum(inputStream, (long) content.length());

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getFileSize()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("计算输入流的SHA256校验和")
    void shouldComputeSha256ForInputStream() throws Exception {
        // Given
        String content = "Hello, World!";
        InputStream inputStream = createInputStream(content);
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("SHA256"));

        // When
        Checksum checksum = checksumService.computeChecksum(inputStream, (long) content.length());

        // Then
        assertThat(checksum.getSha256()).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
        assertThat(checksum.getFileSize()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("同时计算输入流的MD5和SHA256")
    void shouldComputeBothForInputStream() throws Exception {
        // Given
        String content = "Hello, World!";
        InputStream inputStream = createInputStream(content);
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5", "SHA256"));

        // When
        Checksum checksum = checksumService.computeChecksum(inputStream, (long) content.length());

        // Then
        assertThat(checksum.getMd5()).isEqualTo("65a8e27d8879283831b664bd8b7f0ad4");
        assertThat(checksum.getSha256()).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
    }

    @Test
    @DisplayName("验证正确的MD5校验和")
    void shouldVerifyCorrectMd5() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        boolean result = checksumService.verifyChecksum(
            testFile,
            "65a8e27d8879283831b664bd8b7f0ad4",
            null
        );

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("验证错误的MD5校验和")
    void shouldRejectIncorrectMd5() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        boolean result = checksumService.verifyChecksum(
            testFile,
            "incorrectmd5hash",
            null
        );

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("验证正确的SHA256校验和")
    void shouldVerifyCorrectSha256() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("SHA256"));

        // When
        boolean result = checksumService.verifyChecksum(
            testFile,
            null,
            "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        );

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("验证错误的SHA256校验和")
    void shouldRejectIncorrectSha256() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("SHA256"));

        // When
        boolean result = checksumService.verifyChecksum(
            testFile,
            null,
            "incorrectsha256hash"
        );

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("同时验证MD5和SHA256")
    void shouldVerifyBothMd5AndSha256() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5", "SHA256"));

        // When
        Checksum actualChecksum = checksumService.computeChecksum(testFile);
        boolean result = actualChecksum.verify(
            "65a8e27d8879283831b664bd8b7f0ad4",
            "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        );

        // Then
        assertThat(result).isTrue();
        assertThat(actualChecksum.isVerified()).isTrue();
    }

    @Test
    @DisplayName("空算法列表应该跳过所有计算")
    void shouldSkipAllCalculationsWhenAlgorithmsEmpty() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList());

        // When
        Checksum checksum = checksumService.computeChecksum(testFile);

        // Then
        assertThat(checksum.getMd5()).isNull();
        assertThat(checksum.getSha256()).isNull();
        assertThat(checksum.getFileSize()).isEqualTo(content.length());
    }

    @Test
    @DisplayName("大文件的MD5计算（模拟）")
    void shouldComputeMd5ForLargeFile() throws Exception {
        // Given
        Path largeFile = tempDir.resolve("large.bin");
        byte[] largeContent = new byte[10000]; // 10KB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        Files.write(largeFile, largeContent);
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        // When
        Checksum checksum = checksumService.computeChecksum(largeFile);

        // Then
        assertThat(checksum.getMd5()).isNotNull();
        assertThat(checksum.getMd5()).hasSize(32); // MD5 hash length
        assertThat(checksum.getFileSize()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("验证方法应该处理null值")
    void shouldHandleNullValuesInVerify() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        String content = "Hello, World!";
        Files.write(testFile, content.getBytes());
        when(checksumConfig.getAlgorithms()).thenReturn(Arrays.asList("MD5"));

        Checksum checksum = checksumService.computeChecksum(testFile);

        // When - null值应该被认为是匹配的
        boolean result = checksum.verify(null, null);

        // Then
        assertThat(result).isTrue();
    }

    /**
     * 创建模拟输入流
     */
    private InputStream createInputStream(String content) {
        return new java.io.ByteArrayInputStream(content.getBytes());
    }
}
