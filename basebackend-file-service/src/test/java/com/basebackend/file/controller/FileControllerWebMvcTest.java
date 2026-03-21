package com.basebackend.file.controller;

import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.dto.PageResult;
import com.basebackend.file.entity.FileMetadata;
import com.basebackend.file.limit.RateLimiter;
import com.basebackend.file.service.FileManagementService;
import com.basebackend.file.service.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController MockMvc 测试")
class FileControllerWebMvcTest {

    @Mock
    private FileService fileService;

    @Mock
    private FileManagementService fileManagementService;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private FileController fileController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /api/files/list 应按当前用户上下文透传查询参数")
    void shouldPassUserContextForListFiles() throws Exception {
        UserContextHolder.set(UserContext.builder()
                .userId(7L)
                .roles(Set.of("admin"))
                .build());

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOriginalName("report.pdf");
        PageResult<FileMetadata> pageResult = PageResult.of(List.of(metadata), 1L, 1L, 10L);

        when(fileManagementService.listFiles(
                eq("report"),
                eq("pdf"),
                eq(10L),
                eq(99L),
                eq(false),
                eq(null),
                eq(null),
                eq(1L),
                eq(10L),
                eq(7L),
                eq(true)
        )).thenReturn(pageResult);

        MvcResult mvcResult = mockMvc.perform(get("/api/files/list")
                        .param("fileName", "report")
                        .param("fileExtension", "pdf")
                        .param("folderId", "10")
                        .param("ownerId", "99")
                        .param("isPublic", "false")
                        .param("current", "1")
                        .param("size", "10"))
                        .andExpect(status().isOk())
                        .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"total\":1");
        assertThat(responseBody).contains("\"fileId\":\"file-001\"");
        assertThat(responseBody).contains("\"originalName\":\"report.pdf\"");

        verify(fileManagementService).listFiles(
                "report", "pdf", 10L, 99L, false, null, null,
                1L, 10L, 7L, true
        );
    }

    @Test
    @DisplayName("GET /api/files/{fileId} 应按普通用户上下文透传详情请求")
    void shouldPassUserContextForGetFileDetail() throws Exception {
        UserContextHolder.set(UserContext.builder()
                .userId(8L)
                .roles(Set.of("user"))
                .build());

        FileMetadata metadata = new FileMetadata();
        metadata.setFileId("file-001");
        metadata.setOriginalName("manual.docx");

        when(fileManagementService.getFileDetail("file-001", 8L, false)).thenReturn(metadata);

        MvcResult mvcResult = mockMvc.perform(get("/api/files/{fileId}", "file-001"))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        assertThat(responseBody).contains("\"fileId\":\"file-001\"");
        assertThat(responseBody).contains("\"originalName\":\"manual.docx\"");

        verify(fileManagementService).getFileDetail("file-001", 8L, false);
    }
}
