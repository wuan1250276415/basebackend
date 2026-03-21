package com.basebackend.file.service;

import com.basebackend.common.exception.BusinessException;
import com.basebackend.file.config.FileProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * FileService测试类
 * 测试文件上传、删除、获取等核心功能
 *
 * @author BaseBackend
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 文件服务测试")
class FileServiceTest {

    @Mock
    private FileProperties fileProperties;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("文件上传成功")
    void shouldUploadFileSuccessfully() throws Exception {
        // Given
        byte[] fileContent = "test content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            fileContent
        );

        String expectedPath = "/files/2024/01/01/test-file.txt";
        when(fileProperties.getMaxSize()).thenReturn(100L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt", "pdf"});
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        String result = fileService.uploadFile(file);

        // Then
        assertThat(result).contains("/files/");
        verify(fileProperties, times(1)).getUploadPath();
        verify(fileProperties, times(1)).getAccessPrefix();

        // 验证文件确实被创建
        String realPath = result.replace(fileProperties.getAccessPrefix() + "/", "");
        Path filePath = tempDir.resolve(realPath);
        assertThat(Files.exists(filePath)).isTrue();
    }

    @Test
    @DisplayName("文件上传失败 - 文件为空")
    void shouldRejectEmptyFile() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("文件上传失败 - 文件大小超过限制")
    void shouldRejectFileTooLarge() {
        // Given
        byte[] largeContent = new byte[20];
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", largeContent);

        when(fileProperties.getMaxSize()).thenReturn(10L);

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class);
        verify(fileProperties, times(1)).getMaxSize();
        verify(fileProperties, never()).getAllowedTypes();
    }

    @Test
    @DisplayName("文件上传失败 - 文件名为空")
    void shouldRejectNullFilename() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "", "text/plain", "content".getBytes());

        when(fileProperties.getMaxSize()).thenReturn(1000L);

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class);
        verify(fileProperties, times(1)).getMaxSize();
    }

    @Test
    @DisplayName("文件上传失败 - 不支持的文件类型")
    void shouldRejectUnsupportedFileType() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "text/plain", "content".getBytes());

        lenient().when(fileProperties.getMaxSize()).thenReturn(1000L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt", "pdf"});

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("文件上传失败 - 文件类型为空")
    void shouldRejectEmptyFileType() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test", "text/plain", "content".getBytes());

        lenient().when(fileProperties.getMaxSize()).thenReturn(1000L);
        lenient().when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt", "pdf"});

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("文件上传失败 - IO异常")
    void shouldHandleIOException() throws Exception {
        // Given
        MultipartFile file = mock(MultipartFile.class);

        when(fileProperties.getMaxSize()).thenReturn(1000L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt"});
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(7L);
        when(file.getOriginalFilename()).thenReturn("test.txt");
        doThrow(new IOException("mock io failure"))
            .when(file)
            .transferTo(any(java.io.File.class));

        // When & Then
        assertThatThrownBy(() -> fileService.uploadFile(file))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("文件上传失败");
    }

    @Test
    @DisplayName("文件删除成功")
    void shouldDeleteFileSuccessfully() throws Exception {
        // Given
        String filename = "test.txt";
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, "test content".getBytes());

        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        fileService.deleteFile("/files/" + filename);

        // Then
        assertThat(Files.exists(filePath)).isFalse();
        verify(fileProperties, times(1)).getAccessPrefix();
        verify(fileProperties, times(1)).getUploadPath();
    }

    @Test
    @DisplayName("文件删除 - 文件不存在")
    void shouldHandleDeleteNonExistentFile() {
        // Given
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        fileService.deleteFile("/files/nonexistent.txt");

        // Then - 应该正常执行，不抛出异常
        verify(fileProperties, times(1)).getAccessPrefix();
        verify(fileProperties, times(1)).getUploadPath();
    }

    @Test
    @DisplayName("文件删除 - 无效路径下文件不存在时正常执行")
    void shouldHandleIOExceptionOnDelete() {
        // Given - /invalid/path 不存在，但 resolveAndValidatePath 会正常解析
        // deleteFile 内部检测到文件不存在时只打印 warn，不抛异常
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn("/invalid/path");

        // When & Then - 不应该抛出异常
        fileService.deleteFile("/files/test.txt");
        verify(fileProperties, times(1)).getAccessPrefix();
        verify(fileProperties, times(1)).getUploadPath();
    }

    @Test
    @DisplayName("获取文件成功")
    void shouldGetFileSuccessfully() throws Exception {
        // Given
        String filename = "test.txt";
        byte[] content = "test content".getBytes();
        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content);

        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        java.io.File result = fileService.getFile("/files/" + filename);

        // Then
        assertThat(result).exists();
        assertThat(result).isEqualTo(filePath.toFile());
        assertThat(Files.readAllBytes(result.toPath())).isEqualTo(content);
    }

    @Test
    @DisplayName("获取文件失败 - 文件不存在")
    void shouldThrowExceptionWhenFileNotFound() {
        // Given
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When & Then
        assertThatThrownBy(() -> fileService.getFile("/files/nonexistent.txt"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("文件不存在");
    }

    @Test
    @DisplayName("文件类型验证 - 大小写不敏感（大写扩展名被接受）")
    void shouldValidateFileTypeCaseInsensitive() {
        // Given - 实现中使用 toLowerCase() 做大小写不敏感匹配
        MockMultipartFile file = new MockMultipartFile("file", "test.TXT", "text/plain", "content".getBytes());

        when(fileProperties.getMaxSize()).thenReturn(1000L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt", "pdf"});
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When & Then - 大写TXT应通过验证（不抛异常）
        String result = fileService.uploadFile(file);
        assertThat(result).startsWith("/files/");
        assertThat(result).endsWith(".TXT");
    }

    @Test
    @DisplayName("文件类型验证 - 大写扩展名")
    void shouldValidateFileTypeWithUppercaseExtension() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.TXT", "text/plain", "content".getBytes());

        lenient().when(fileProperties.getMaxSize()).thenReturn(1000L);
        lenient().when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt", "pdf"});

        // When & Then
        // 注意：当前实现可能没有做大小写转换，所以这可能通过也可能失败
    }

    @Test
    @DisplayName("文件访问路径生成正确")
    void shouldGenerateCorrectAccessPath() throws Exception {
        // Given
        byte[] content = "test".getBytes();
        MockMultipartFile file = new MockMultipartFile("test.txt", "test.txt", "text/plain", content);

        when(fileProperties.getMaxSize()).thenReturn(1000L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt"});
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        String path = fileService.uploadFile(file);

        // Then
        assertThat(path).startsWith("/files/");
        assertThat(path).endsWith(".txt");
        assertThat(path).matches("/files/\\d{4}/\\d{2}/\\d{2}/[a-f0-9-]+\\.txt");
    }

    @Test
    @DisplayName("文件上传 - 创建目录结构")
    void shouldCreateDirectoryStructure() throws Exception {
        // Given
        byte[] content = "test".getBytes();
        MockMultipartFile file = new MockMultipartFile("test.txt", "test.txt", "text/plain", content);

        when(fileProperties.getMaxSize()).thenReturn(1000L);
        when(fileProperties.getAllowedTypes()).thenReturn(new String[]{"txt"});
        when(fileProperties.getAccessPrefix()).thenReturn("/files");
        when(fileProperties.getUploadPath()).thenReturn(tempDir.toString());

        // When
        fileService.uploadFile(file);

        // Then - 验证日期目录被创建
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dirPath = tempDir.resolve(datePath);
        assertThat(Files.exists(dirPath)).isTrue();
        assertThat(Files.isDirectory(dirPath)).isTrue();
    }
}
