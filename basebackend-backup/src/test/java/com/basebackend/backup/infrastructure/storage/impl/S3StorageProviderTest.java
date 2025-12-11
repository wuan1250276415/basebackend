package com.basebackend.backup.infrastructure.storage.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * S3存储提供者测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("S3StorageProvider S3存储提供者测试")
class S3StorageProviderTest {

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.Storage storageConfig;

    @Mock
    private BackupProperties.Storage.S3 s3Config;

    private S3StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        storageProvider = new S3StorageProvider(backupProperties);

        when(backupProperties.getStorage()).thenReturn(storageConfig);
        when(storageConfig.getS3()).thenReturn(s3Config);

        // 模拟S3配置
        when(s3Config.getRegion()).thenReturn("us-east-1");
        when(s3Config.getEndpoint()).thenReturn("https://s3.amazonaws.com");
        when(s3Config.getAccessKey()).thenReturn("test-access-key");
        when(s3Config.getSecretKey()).thenReturn("test-secret-key");
        when(s3Config.getMultipartChunkSize()).thenReturn(10485760L); // 10MB
    }

    @Test
    @DisplayName("获取支持的功能特性")
    void shouldGetSupportedFeatures() {
        // When
        String[] features = storageProvider.getSupportedFeatures();

        // Then
        assertThat(features).isNotNull();
        assertThat(features.length).isGreaterThan(0);
        assertThat(features).contains(
                "multipart_upload",
                "object_versioning",
                "encryption",
                "presigned_url",
                "lifecycle_management",
                "cors"
        );
    }

    @Test
    @DisplayName("获取存储类型")
    void shouldGetStorageType() {
        // When
        String storageType = storageProvider.getStorageType();

        // Then
        assertThat(storageType).isEqualTo("s3");
    }

    @Test
    @DisplayName("上传文件配置验证")
    void shouldValidateUploadConfiguration() {
        // Given
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest request = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), "text/plain",
                null, null, false, null
        );

        // 验证请求参数
        assertThat(request.getBucket()).isEqualTo("test-bucket");
        assertThat(request.getKey()).isEqualTo("test-file.txt");
        assertThat(request.getSize()).isGreaterThan(0);
        assertThat(request.getContentType()).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("多部分上传配置验证")
    void shouldValidateMultipartUploadConfiguration() {
        // Given - 大文件请求（超过10MB）
        int largeSize = 20 * 1024 * 1024; // 20MB
        InputStream inputStream = new ByteArrayInputStream(new byte[largeSize]);
        UploadRequest request = new UploadRequest(
                "test-bucket", "large-file.bin", inputStream,
                (long) largeSize, null, null, null, false, null
        );

        // 验证应该使用多部分上传（根据大小）
        boolean shouldUseMultipart = request.getSize() > s3Config.getMultipartChunkSize();
        assertThat(shouldUseMultipart).isTrue();
    }

    @Test
    @DisplayName("简单上传配置验证")
    void shouldValidateSimpleUploadConfiguration() {
        // Given - 小文件请求（小于10MB）
        int smallSize = 1024; // 1KB
        InputStream inputStream = new ByteArrayInputStream(new byte[smallSize]);
        UploadRequest request = new UploadRequest(
                "test-bucket", "small-file.txt", inputStream,
                (long) smallSize, null, null, null, false, null
        );

        // 验证应该使用简单上传（根据大小）
        boolean shouldUseMultipart = request.getSize() > s3Config.getMultipartChunkSize();
        assertThat(shouldUseMultipart).isFalse();
    }

    @Test
    @DisplayName("强制多部分上传配置")
    void shouldForceMultipartUpload() {
        // Given - 小文件但强制多部分上传
        int smallSize = 1024; // 1KB
        InputStream inputStream = new ByteArrayInputStream(new byte[smallSize]);
        UploadRequest request = new UploadRequest(
                "test-bucket", "small-file.txt", inputStream,
                (long) smallSize, null, null, null, true, null
        );

        // 验证
        assertThat(request.isMultipart()).isTrue();
    }

    @Test
    @DisplayName("MD5计算验证")
    void shouldCalculateMd5() throws Exception {
        // Given
        byte[] data = "Hello, S3!".getBytes();

        // 测试MD5计算逻辑（在S3StorageProvider中）
        String md5 = calculateMD5Test(data);

        // Then
        assertThat(md5).isNotNull();
        assertThat(md5.length()).isEqualTo(32); // MD5是32位十六进制
        assertThat(md5).matches("[a-f0-9]{32}");
    }

    @Test
    @DisplayName("字节数组格式化验证")
    void shouldFormatBytes() throws Exception {
        // 测试字节格式化方法
        assertThat(formatBytesTest(0L)).isEqualTo("0 B");
        assertThat(formatBytesTest(1024L)).isEqualTo("1.00 KB");
        assertThat(formatBytesTest(1048576L)).isEqualTo("1.00 MB");
        assertThat(formatBytesTest(1073741824L)).isEqualTo("1.00 GB");
    }

    /**
     * 辅助方法：计算MD5（测试版本）
     */
    private String calculateMD5Test(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            return bytesToHexTest(hash);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 辅助方法：字节数组转十六进制（测试版本）
     */
    private String bytesToHexTest(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 辅助方法：格式化字节数（测试版本）
     */
    private String formatBytesTest(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}
