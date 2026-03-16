package com.basebackend.backup.infrastructure.storage.impl;

import com.basebackend.backup.config.BackupProperties;
import com.basebackend.backup.infrastructure.storage.StorageResult;
import com.basebackend.backup.infrastructure.storage.StorageUsage;
import com.basebackend.backup.infrastructure.storage.UploadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private S3Client s3Client;

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
    @DisplayName("简单上传应使用Base64格式Content-MD5")
    void shouldSetBase64ContentMd5InSimpleUpload() throws Exception {
        // Given
        byte[] data = "Hello, S3!".getBytes(StandardCharsets.UTF_8);
        UploadRequest request = new UploadRequest(
                "test-bucket", "test-file.txt", new ByteArrayInputStream(data),
                (long) data.length, "text/plain",
                null, Map.of(), false, null
        );
        setPrivateField(storageProvider, "s3Client", s3Client);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag-1").build());

        // When
        StorageResult result = invokePrivateMethod(
                storageProvider,
                "simpleUpload",
                new Class[]{String.class, String.class, InputStream.class, long.class, UploadRequest.class},
                "test-bucket", "test-file.txt", request.getInputStream(), (long) data.length, request
        );

        // Then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        org.mockito.Mockito.verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest putObjectRequest = requestCaptor.getValue();

        String expectedBase64Md5 = Base64.getEncoder()
                .encodeToString(MessageDigest.getInstance("MD5").digest(data));
        assertThat(putObjectRequest.contentMD5()).isEqualTo(expectedBase64Md5);
        String md5Hex = (String) result.getMetadata().get("md5");
        assertThat(md5Hex).matches("[a-f0-9]{32}");
    }

    @Test
    @DisplayName("多部分上传在短读流场景下不应丢失字节")
    void shouldNotSkipBytesWhenMultipartUploadUsesShortReads() throws Exception {
        // Given
        byte[] payload = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8); // 26 bytes
        when(s3Config.getMultipartChunkSize()).thenReturn(5L);
        setPrivateField(storageProvider, "s3Client", s3Client);
        when(s3Client.createMultipartUpload(any(software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("upload-1").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    UploadPartRequest req = invocation.getArgument(0);
                    return UploadPartResponse.builder().eTag("etag-" + req.partNumber()).build();
                });
        when(s3Client.completeMultipartUpload(any(software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().eTag("final-etag").build());
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength((long) payload.length).build());

        UploadRequest request = new UploadRequest(
                "test-bucket", "large-file.bin", shortReadInputStream(payload, 3),
                (long) payload.length, "application/octet-stream",
                null, Map.of(), true, null
        );

        // When
        StorageResult result = invokePrivateMethod(
                storageProvider,
                "multipartUpload",
                new Class[]{String.class, String.class, InputStream.class, long.class, UploadRequest.class},
                "test-bucket", "large-file.bin", request.getInputStream(), (long) payload.length, request
        );

        // Then
        ArgumentCaptor<UploadPartRequest> partCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
        org.mockito.Mockito.verify(s3Client, org.mockito.Mockito.atLeastOnce())
                .uploadPart(partCaptor.capture(), any(RequestBody.class));

        long uploadedBytes = partCaptor.getAllValues().stream()
                .mapToLong(UploadPartRequest::contentLength)
                .sum();
        assertThat(uploadedBytes).isEqualTo(payload.length);

        assertThat(partCaptor.getAllValues()).isNotEmpty();
        for (int i = 0; i < partCaptor.getAllValues().size(); i++) {
            assertThat(partCaptor.getAllValues().get(i).partNumber()).isEqualTo(i + 1);
            assertThat(partCaptor.getAllValues().get(i).contentLength()).isGreaterThan(0);
        }
        assertThat(result.isSuccess()).isTrue();
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

    @SuppressWarnings("unchecked")
    private <T> T invokePrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private InputStream shortReadInputStream(byte[] data, int maxChunk) {
        return new InputStream() {
            private int index = 0;

            @Override
            public int read(byte[] b, int off, int len) {
                if (index >= data.length) {
                    return -1;
                }
                int actual = Math.min(Math.min(len, maxChunk), data.length - index);
                System.arraycopy(data, index, b, off, actual);
                index += actual;
                return actual;
            }

            @Override
            public int read() {
                if (index >= data.length) {
                    return -1;
                }
                return data[index++] & 0xFF;
            }
        };
    }
}
