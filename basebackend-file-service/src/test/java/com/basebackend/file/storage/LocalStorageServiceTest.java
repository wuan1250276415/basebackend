package com.basebackend.file.storage;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.FileProperties;
import com.basebackend.file.storage.impl.LocalStorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * LocalStorageServiceImpl测试类
 * 测试本地存储服务功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LocalStorageService 本地存储服务测试")
class LocalStorageServiceTest {

    @Mock
    private FileProperties fileProperties;

    @InjectMocks
    private LocalStorageServiceImpl localStorageService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("上传文件成功")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        String path = "test/file.txt";
        byte[] content = "test content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        String contentType = "text/plain";
        long size = content.length;

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        String result = localStorageService.upload(inputStream, path, contentType, size);

        // Then
        assertThat(result).isNotEmpty();
        Path filePath = tempDir.resolve(path);
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.readAllBytes(filePath)).isEqualTo(content);
    }

    @Test
    @DisplayName("上传文件 - 创建父目录")
    void shouldCreateParentDirectories() throws Exception {
        // Given
        String path = "nested/dir/file.txt";
        byte[] content = "test".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.upload(inputStream, path, "text/plain", content.length);

        // Then
        Path parentDir = tempDir.resolve("nested/dir");
        assertThat(Files.exists(parentDir)).isTrue();
        assertThat(Files.isDirectory(parentDir)).isTrue();
    }

    @Test
    @DisplayName("上传文件 - 覆盖已存在文件")
    void shouldReplaceExistingFile() throws Exception {
        // Given
        String path = "test/file.txt";
        byte[] oldContent = "old content".getBytes();
        byte[] newContent = "new content".getBytes();

        // 创建已存在的文件
        Path filePath = tempDir.resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, oldContent);

        InputStream inputStream = new ByteArrayInputStream(newContent);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.upload(inputStream, path, "text/plain", newContent.length);

        // Then
        assertThat(Files.readAllBytes(filePath)).isEqualTo(newContent);
    }

    @Test
    @DisplayName("上传文件失败 - IO异常")
    void shouldThrowExceptionOnIOException() throws Exception {
        // Given
        String path = "invalid/file.txt";
        InputStream inputStream = new ByteArrayInputStream("test".getBytes());

        when(fileProperties.getUploadPath()).thenReturn("/invalid/path");

        // When & Then
        assertThatThrownBy(() -> localStorageService.upload(inputStream, path, "text/plain", 4))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("文件上传失败");
    }

    @Test
    @DisplayName("下载文件成功")
    void shouldDownloadFileSuccessfully() throws Exception {
        // Given
        String path = "test/file.txt";
        byte[] content = "test content".getBytes();

        // 创建文件
        Path filePath = tempDir.resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        InputStream result = localStorageService.download(path);

        // Then
        assertThat(result).isNotNull();
        byte[] readContent = result.readAllBytes();
        assertThat(readContent).isEqualTo(content);
    }

    @Test
    @DisplayName("下载文件 - 文件不存在")
    void shouldReturnNullWhenFileNotExists() {
        // Given
        String path = "nonexistent/file.txt";

        lenient().when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        InputStream result = localStorageService.download(path);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("删除文件成功")
    void shouldDeleteFileSuccessfully() throws Exception {
        // Given
        String path = "test/file.txt";
        byte[] content = "test content".getBytes();

        // 创建文件
        Path filePath = tempDir.resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.delete(path);

        // Then
        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    @DisplayName("删除文件 - 文件不存在")
    void shouldHandleDeleteNonExistentFile() {
        // Given
        String path = "nonexistent/file.txt";

        lenient().when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When - 不应该抛出异常
        localStorageService.delete(path);

        // Then - 正常执行
    }

    @Test
    @DisplayName("复制文件成功")
    void shouldCopyFileSuccessfully() throws Exception {
        // Given
        String sourcePath = "test/source.txt";
        String targetPath = "test/target.txt";
        byte[] content = "test content".getBytes();

        // 创建源文件
        Path sourceFile = tempDir.resolve(sourcePath);
        Files.createDirectories(sourceFile.getParent());
        Files.write(sourceFile, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.copy(sourcePath, targetPath);

        // Then
        Path targetFile = tempDir.resolve(targetPath);
        assertThat(Files.exists(targetFile)).isTrue();
        assertThat(Files.readAllBytes(targetFile)).isEqualTo(content);
        assertThat(Files.exists(sourceFile)).isTrue(); // 源文件应该还存在
    }

    @Test
    @DisplayName("复制文件 - 创建目标目录")
    void shouldCreateTargetDirectoryOnCopy() throws Exception {
        // Given
        String sourcePath = "source.txt";
        String targetPath = "nested/dir/target.txt";
        byte[] content = "test".getBytes();

        // 创建源文件
        Path sourceFile = tempDir.resolve(sourcePath);
        Files.write(sourceFile, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.copy(sourcePath, targetPath);

        // Then
        Path targetFile = tempDir.resolve(targetPath);
        assertThat(Files.exists(targetFile)).isTrue();
    }

    @Test
    @DisplayName("复制文件失败 - 源文件不存在")
    void shouldThrowExceptionWhenSourceNotExists() {
        // Given
        String sourcePath = "nonexistent/source.txt";
        String targetPath = "test/target.txt";

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When & Then
        assertThatThrownBy(() -> localStorageService.copy(sourcePath, targetPath))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("移动文件成功")
    void shouldMoveFileSuccessfully() throws Exception {
        // Given
        String sourcePath = "test/source.txt";
        String targetPath = "test/target.txt";
        byte[] content = "test content".getBytes();

        // 创建源文件
        Path sourceFile = tempDir.resolve(sourcePath);
        Files.createDirectories(sourceFile.getParent());
        Files.write(sourceFile, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        localStorageService.move(sourcePath, targetPath);

        // Then
        Path targetFile = tempDir.resolve(targetPath);
        assertThat(Files.exists(targetFile)).isTrue();
        assertThat(Files.exists(sourceFile)).isFalse(); // 源文件应该被移动
    }

    @Test
    @DisplayName("移动文件失败 - 源文件不存在")
    void shouldThrowExceptionWhenSourceNotExistsOnMove() {
        // Given
        String sourcePath = "nonexistent/source.txt";
        String targetPath = "test/target.txt";

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When & Then
        assertThatThrownBy(() -> localStorageService.move(sourcePath, targetPath))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("检查文件是否存在 - 存在")
    void shouldReturnTrueWhenFileExists() throws Exception {
        // Given
        String path = "test/file.txt";
        byte[] content = "test".getBytes();

        Path filePath = tempDir.resolve(path);
        Files.createDirectories(filePath.getParent());
        Files.write(filePath, content);

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        boolean exists = localStorageService.exists(path);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("检查文件是否存在 - 不存在")
    void shouldReturnFalseWhenFileNotExists() {
        // Given
        String path = "nonexistent/file.txt";

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        boolean exists = localStorageService.exists(path);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("获取文件URL")
    void shouldGetFileUrl() {
        // Given
        String path = "test/file.txt";

        lenient().when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        String url = localStorageService.getUrl(path);

        // Then
        assertThat(url).isNotEmpty();
        assertThat(url).endsWith(path);
    }

    @Test
    @DisplayName("获取预签名URL - 本地存储返回普通URL")
    void shouldGetPresignedUrl() {
        // Given
        String path = "test/file.txt";
        int expireTime = 3600;

        lenient().when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        String presignedUrl = localStorageService.getPresignedUrl(path, expireTime);

        // Then - 本地存储应该返回普通URL
        assertThat(presignedUrl).isNotEmpty();
    }

    @Test
    @DisplayName("列出文件")
    void shouldListFiles() throws Exception {
        // Given
        String prefix = "test/";
        Path testDir = tempDir.resolve("test");
        Files.createDirectories(testDir);
        Files.write(testDir.resolve("file1.txt"), "content1".getBytes());
        Files.write(testDir.resolve("file2.txt"), "content2".getBytes());
        Files.createDirectories(testDir.resolve("sub"));
        Files.write(testDir.resolve("sub").resolve("file3.txt"), "content3".getBytes());
        Files.write(tempDir.resolve("other.txt"), "other".getBytes()); // 不应该列出

        lenient().when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        List<String> files = localStorageService.listFiles(prefix);

        // Then
        assertThat(files).hasSize(3);
        assertThat(files).allMatch(f -> f.startsWith(prefix));
    }

    @Test
    @DisplayName("列出文件 - 空目录")
    void shouldReturnEmptyListForEmptyDirectory() {
        // Given
        String prefix = "empty/";

        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        List<String> files = localStorageService.listFiles(prefix);

        // Then
        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("获取存储类型")
    void shouldGetStorageType() {
        // When
        StorageService.StorageType type = localStorageService.getStorageType();

        // Then
        assertThat(type).isEqualTo(StorageService.StorageType.LOCAL);
    }
}
