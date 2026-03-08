package com.basebackend.storage.provider;

import com.basebackend.storage.config.StorageProperties;
import com.basebackend.storage.exception.StorageException;
import com.basebackend.storage.model.UploadRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalStorageProvider 安全测试
 */
class LocalStorageProviderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("上传时应拒绝目录穿越 key")
    void shouldRejectPathTraversalOnUpload() {
        LocalStorageProvider provider = buildProvider();
        UploadRequest request = UploadRequest.builder()
                .bucket("default")
                .key("../outside.txt")
                .inputStream(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)))
                .size(4L)
                .contentType("text/plain")
                .build();

        assertThatThrownBy(() -> provider.upload(request))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("访问被拒绝");
    }

    @Test
    @DisplayName("下载时应拒绝目录穿越 key")
    void shouldRejectPathTraversalOnDownload() {
        LocalStorageProvider provider = buildProvider();

        assertThatThrownBy(() -> provider.download("default", "../../etc/passwd"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("访问被拒绝");
    }

    @Test
    @DisplayName("上传和下载正常路径应可用")
    void shouldSupportNormalPath() throws Exception {
        LocalStorageProvider provider = buildProvider();
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        UploadRequest request = UploadRequest.builder()
                .bucket("default")
                .key("safe/path.txt")
                .inputStream(new ByteArrayInputStream(content))
                .size((long) content.length)
                .contentType("text/plain")
                .build();

        provider.upload(request);
        try (InputStream in = provider.download("default", "safe/path.txt")) {
            byte[] read = in.readAllBytes();
            assertThat(read).isEqualTo(content);
        }
    }

    private LocalStorageProvider buildProvider() {
        StorageProperties properties = new StorageProperties();
        properties.setDefaultBucket("default");
        properties.getLocal().setBasePath(tempDir.toString());
        properties.getLocal().setUrlPrefix("/files");
        return new LocalStorageProvider(properties);
    }
}
