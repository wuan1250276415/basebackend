package com.basebackend.file.controller;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.model.Result;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.limit.RateLimiter;
import com.basebackend.file.service.FileManagementService;
import com.basebackend.file.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController 用户上下文测试")
class FileControllerContextTest {

    @Mock
    private FileService fileService;

    @Mock
    private FileManagementService fileManagementService;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private FileController fileController;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("文件列表接口应将当前用户与超管标记传递给服务层")
    void shouldPassCurrentUserContextToListFiles() {
        UserContextHolder.set(UserContext.builder()
                .userId(7L)
                .roles(Set.of("admin"))
                .build());

        PageResult<FileMetadata> expectedPage = PageResult.of(List.of(), 0, 1, 10);
        when(fileManagementService.listFiles(
                "report", "pdf", 10L, 99L, false, null, null,
                1L, 10L, 7L, true
        )).thenReturn(expectedPage);

        Result<PageResult<FileMetadata>> result = fileController.getFileList(
                "report", "pdf", 10L, 99L, false, null, null, 1, 10
        );

        assertThat(result.getData()).isSameAs(expectedPage);
        verify(fileManagementService).listFiles(
                "report", "pdf", 10L, 99L, false, null, null,
                1L, 10L, 7L, true
        );
    }

    @Test
    @DisplayName("文件详情接口应将当前用户与普通用户标记传递给服务层")
    void shouldPassCurrentUserContextToGetFileDetail() {
        UserContextHolder.set(UserContext.builder()
                .userId(8L)
                .roles(Set.of("user"))
                .build());

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        when(fileManagementService.getFileDetail("file-001", 8L, false)).thenReturn(metadata);

        Result<FileMetadata> result = fileController.getFileDetail("file-001");

        assertThat(result.getData()).isSameAs(metadata);
        verify(fileManagementService).getFileDetail("file-001", 8L, false);
    }
}
