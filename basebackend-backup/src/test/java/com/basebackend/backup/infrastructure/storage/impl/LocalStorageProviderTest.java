package com.basebackend.backup.infrastructure.storage.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 本地存储提供者测试
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LocalStorageProvider 本地存储提供者测试")
class LocalStorageProviderTest {

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.Storage.Local localConfig;

    @TempDir
    Path tempDir;

    private LocalStorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        storageProvider = new LocalStorageProvider(backupProperties);

        when(backupProperties.getStorage()).thenReturn(mock(BackupProperties.Storage.class));
        when(backupProperties.getStorage().getLocal()).thenReturn(localConfig);
        when(localConfig.getBasePath()).thenReturn(tempDir.toString());
    }

    @Test
    @DisplayName("上传文件成功")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest request = new UploadRequest(
                "test-bucket",           // bucket
                "test-file.txt",         // key
                inputStream,             // inputStream
                (long) content.length(), // size
                "text/plain",            // contentType
                null,                    // md5Checksum
                null,                    // metadata
                false,                   // multipart
                null                     // chunkSize
        );

        // When
        StorageResult result = storageProvider.upload(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBucket()).isEqualTo("test-bucket");
        assertThat(result.getKey()).isEqualTo("test-file.txt");
        assertThat(result.getStorageType()).isEqualTo("local");
        assertThat(result.getSize()).isEqualTo(content.length());
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).containsKey("md5");
        assertThat(result.getMetadata()).containsKey("sha256");
        assertThat(result.getLocation()).isNotNull();
    }

    @Test
    @DisplayName("下载文件成功")
    void shouldDownloadFileSuccessfully() throws Exception {
        // Given - 先上传一个文件
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest uploadRequest = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        storageProvider.upload(uploadRequest);

        // When
        InputStream downloadedStream = storageProvider.download("test-bucket", "test-file.txt");

        // Then
        assertThat(downloadedStream).isNotNull();
        byte[] downloadedBytes = downloadedStream.readAllBytes();
        assertThat(new String(downloadedBytes)).isEqualTo(content);
    }

    @Test
    @DisplayName("下载不存在的文件应该抛出异常")
    void shouldThrowExceptionWhenDownloadingNonExistentFile() {
        // When & Then
        assertThatThrownBy(() -> storageProvider.download("test-bucket", "non-existent.txt"))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("删除存在的文件")
    void shouldDeleteExistingFile() throws Exception {
        // Given - 先上传一个文件
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest uploadRequest = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        storageProvider.upload(uploadRequest);

        // When
        boolean deleted = storageProvider.delete("test-bucket", "test-file.txt");

        // Then
        assertThat(deleted).isTrue();
    }

    @Test
    @DisplayName("删除不存在的文件")
    void shouldReturnFalseWhenDeletingNonExistentFile() throws Exception {
        // When
        boolean deleted = storageProvider.delete("test-bucket", "non-existent.txt");

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("检查存在的文件")
    void shouldCheckExistingFile() throws Exception {
        // Given - 先上传一个文件
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest uploadRequest = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        storageProvider.upload(uploadRequest);

        // When
        boolean exists = storageProvider.exists("test-bucket", "test-file.txt");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查不存在的文件")
    void shouldCheckNonExistentFile() throws Exception {
        // When
        boolean exists = storageProvider.exists("test-bucket", "non-existent.txt");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("验证文件完整性 - MD5匹配")
    void shouldVerifyFileIntegrityWithMatchingMd5() throws Exception {
        // Given - 先上传一个文件
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest uploadRequest = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        StorageResult result = storageProvider.upload(uploadRequest);
        String md5 = (String) result.getMetadata().get("md5");

        // When
        boolean verified = storageProvider.verify("test-bucket", "test-file.txt", md5, null);

        // Then
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("验证文件完整性 - MD5不匹配")
    void shouldVerifyFileIntegrityWithMismatchedMd5() throws Exception {
        // Given - 先上传一个文件
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest uploadRequest = new UploadRequest(
                "test-bucket", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        storageProvider.upload(uploadRequest);

        // When
        boolean verified = storageProvider.verify("test-bucket", "test-file.txt", "incorrect-md5", null);

        // Then
        assertThat(verified).isFalse();
    }

    @Test
    @DisplayName("获取存储使用量")
    void shouldGetStorageUsage() throws Exception {
        // Given - 上传一些文件
        String content = "Test file content";
        for (int i = 0; i < 3; i++) {
            InputStream inputStream = new ByteArrayInputStream(content.getBytes());
            UploadRequest uploadRequest = new UploadRequest(
                    "test-bucket", "file-" + i + ".txt", inputStream,
                    (long) content.length(), null, null, null, false, null
            );
            storageProvider.upload(uploadRequest);
        }

        // When
        StorageUsage usage = storageProvider.getUsage("test-bucket");

        // Then
        assertThat(usage).isNotNull();
        assertThat(usage.getUsedBytes()).isGreaterThan(0L);
        assertThat(usage.getObjectCount()).isEqualTo(3L);
        assertThat(usage.getBucketCount()).isEqualTo(1L);
        assertThat(usage.getStorageType()).isEqualTo("local");
        assertThat(usage.getUsedHumanReadable()).isNotNull();
        assertThat(usage.getTotalBytes()).isEqualTo(-1L); // 本地存储无配额限制
    }

    @Test
    @DisplayName("获取支持的功能特性")
    void shouldGetSupportedFeatures() {
        // When
        String[] features = storageProvider.getSupportedFeatures();

        // Then
        assertThat(features).isNotNull();
        assertThat(features).contains("direct_access", "checksum_verification");
    }

    @Test
    @DisplayName("获取存储类型")
    void shouldGetStorageType() {
        // When
        String storageType = storageProvider.getStorageType();

        // Then
        assertThat(storageType).isEqualTo("local");
    }

    @Test
    @DisplayName("路径验证 - 空bucket应该抛出异常")
    void shouldThrowExceptionForEmptyBucket() throws Exception {
        // Given
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest request = new UploadRequest(
                "", "test-file.txt", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        // When & Then
        assertThatThrownBy(() -> storageProvider.upload(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("存储桶名称不能为空");
    }

    @Test
    @DisplayName("路径验证 - 空key应该抛出异常")
    void shouldThrowExceptionForEmptyKey() throws Exception {
        // Given
        String content = "Test file content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        UploadRequest request = new UploadRequest(
                "test-bucket", "", inputStream,
                (long) content.length(), null, null, null, false, null
        );

        // When & Then
        assertThatThrownBy(() -> storageProvider.upload(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("对象键名不能为空");
    }
}
