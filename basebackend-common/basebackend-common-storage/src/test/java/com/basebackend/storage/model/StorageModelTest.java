package com.basebackend.storage.model;

import com.basebackend.storage.config.StorageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Storage 模型 + 配置单元测试
 */
class StorageModelTest {

    // ========== StorageResult ==========

    @Nested
    @DisplayName("StorageResult")
    class StorageResultTest {

        @Test
        @DisplayName("success 工厂方法")
        void shouldCreateSuccess() {
            var result = StorageResult.success("my-bucket", "file.txt", "/storage/file.txt");
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getBucket()).isEqualTo("my-bucket");
            assertThat(result.getKey()).isEqualTo("file.txt");
            assertThat(result.getLocation()).isEqualTo("/storage/file.txt");
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("failure 工厂方法")
        void shouldCreateFailure() {
            var result = StorageResult.failure("磁盘空间不足");
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorMessage()).isEqualTo("磁盘空间不足");
            assertThat(result.getBucket()).isNull();
        }

        @Test
        @DisplayName("Builder 完整构建")
        void shouldBuildComplete() {
            var result = StorageResult.builder()
                    .bucket("b").key("k").location("/l")
                    .etag("etag-123").versionId("v1")
                    .size(1024L).region("cn-east-1")
                    .accessUrl("https://cdn.example.com/k")
                    .metadata(Map.of("author", "test"))
                    .build();
            assertThat(result.getEtag()).isEqualTo("etag-123");
            assertThat(result.getSize()).isEqualTo(1024L);
            assertThat(result.getRegion()).isEqualTo("cn-east-1");
            assertThat(result.getAccessUrl()).contains("cdn.example.com");
            assertThat(result.getMetadata()).containsEntry("author", "test");
        }

        @Test
        @DisplayName("默认 success=true")
        void shouldDefaultToSuccess() {
            var result = StorageResult.builder().build();
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ========== UploadRequest ==========

    @Nested
    @DisplayName("UploadRequest")
    class UploadRequestTest {

        @Test
        @DisplayName("of(bucket, key, stream, size, contentType) 工厂方法")
        void shouldCreateWithBucket() {
            var stream = new ByteArrayInputStream("hello".getBytes());
            var req = UploadRequest.of("bucket", "file.txt", stream, 5L, "text/plain");
            assertThat(req.getBucket()).isEqualTo("bucket");
            assertThat(req.getKey()).isEqualTo("file.txt");
            assertThat(req.getSize()).isEqualTo(5L);
            assertThat(req.getContentType()).isEqualTo("text/plain");
            assertThat(req.isMultipart()).isFalse();
        }

        @Test
        @DisplayName("of(key, stream, size, contentType) 无桶版本")
        void shouldCreateWithoutBucket() {
            var stream = new ByteArrayInputStream("data".getBytes());
            var req = UploadRequest.of("doc.pdf", stream, 4L, "application/pdf");
            assertThat(req.getBucket()).isNull();
            assertThat(req.getKey()).isEqualTo("doc.pdf");
        }

        @Test
        @DisplayName("默认分块大小 16MB")
        void shouldHaveDefaultChunkSize() {
            var req = UploadRequest.builder().build();
            assertThat(req.getChunkSize()).isEqualTo(16L * 1024 * 1024);
            assertThat(req.isMultipart()).isFalse();
        }

        @Test
        @DisplayName("Builder 设置分块上传")
        void shouldEnableMultipart() {
            var req = UploadRequest.builder()
                    .multipart(true).chunkSize(8L * 1024 * 1024).build();
            assertThat(req.isMultipart()).isTrue();
            assertThat(req.getChunkSize()).isEqualTo(8L * 1024 * 1024);
        }

        @Test
        @DisplayName("支持自定义元数据")
        void shouldSupportMetadata() {
            var req = UploadRequest.builder()
                    .metadata(Map.of("tag", "important")).build();
            assertThat(req.getMetadata()).containsEntry("tag", "important");
        }

        @Test
        @DisplayName("支持校验和")
        void shouldSupportChecksum() {
            var req = UploadRequest.builder()
                    .md5Checksum("abc123").sha256Checksum("def456").build();
            assertThat(req.getMd5Checksum()).isEqualTo("abc123");
            assertThat(req.getSha256Checksum()).isEqualTo("def456");
        }
    }

    // ========== StorageProperties ==========

    @Nested
    @DisplayName("StorageProperties")
    class StoragePropertiesTest {

        @Test
        @DisplayName("默认值正确")
        void shouldHaveCorrectDefaults() {
            var props = new StorageProperties();
            assertThat(props.getType()).isEqualTo("local");
            assertThat(props.getDefaultBucket()).isEqualTo("default");
        }

        @Test
        @DisplayName("Local 默认配置")
        void shouldHaveLocalDefaults() {
            var local = new StorageProperties.Local();
            assertThat(local.getBasePath()).isEqualTo("./storage");
            assertThat(local.getUrlPrefix()).isEqualTo("/files");
            assertThat(local.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Minio 默认禁用")
        void shouldHaveMinioDefaultDisabled() {
            var minio = new StorageProperties.Minio();
            assertThat(minio.isEnabled()).isFalse();
            assertThat(minio.getBucket()).isEqualTo("default");
            assertThat(minio.isSecure()).isFalse();
        }

        @Test
        @DisplayName("OSS 默认禁用")
        void shouldHaveOssDefaultDisabled() {
            var oss = new StorageProperties.Oss();
            assertThat(oss.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("S3 默认配置")
        void shouldHaveS3Defaults() {
            var s3 = new StorageProperties.S3();
            assertThat(s3.isEnabled()).isFalse();
            assertThat(s3.getRegion()).isEqualTo("us-east-1");
            assertThat(s3.isPathStyleAccessEnabled()).isFalse();
        }

        @Test
        @DisplayName("Checksum 默认启用 MD5+SHA256")
        void shouldHaveChecksumDefaults() {
            var checksum = new StorageProperties.ChecksumConfig();
            assertThat(checksum.isEnabled()).isTrue();
            assertThat(checksum.getAlgorithms()).containsExactly("MD5", "SHA256");
        }

        @Test
        @DisplayName("云存储互斥：只有一个 enabled")
        void shouldOnlyOneCloudEnabled() {
            var props = new StorageProperties();
            // 默认都是 false
            assertThat(props.getMinio().isEnabled()).isFalse();
            assertThat(props.getOss().isEnabled()).isFalse();
            assertThat(props.getS3().isEnabled()).isFalse();
            assertThat(props.getLocal().isEnabled()).isTrue();
        }
    }
}
