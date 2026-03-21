package com.basebackend.file.controller;

import com.basebackend.file.limit.RateLimiter;
import com.basebackend.file.service.FileManagementService;
import com.basebackend.file.service.FileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController 废弃接口回归测试")
class FileControllerDeprecatedEndpointsTest {

    private static final String DISABLED_MESSAGE = "旧版按路径接口已禁用，请使用基于 fileId 的受控接口";

    @Mock
    private FileService fileService;

    @Mock
    private FileManagementService fileManagementService;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private FileController fileController;

    @Test
    @DisplayName("GET /api/files/download 应返回 410 并拒绝旧版按路径下载")
    void shouldRejectLegacyPathDownloadEndpoint() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();

        MvcResult mvcResult = mockMvc.perform(get("/api/files/download").param("path", "/tmp/test.txt"))
                .andExpect(status().isGone())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).contains("\"code\":410");
        assertThat(mvcResult.getResponse().getContentAsString()).contains(DISABLED_MESSAGE);
        verifyNoInteractions(fileService, fileManagementService, rateLimiter);
    }

    @Test
    @DisplayName("DELETE /api/files/delete 应返回 410 并拒绝旧版按路径删除")
    void shouldRejectLegacyPathDeleteEndpoint() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();

        MvcResult mvcResult = mockMvc.perform(delete("/api/files/delete").param("path", "/tmp/test.txt"))
                .andExpect(status().isGone())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).contains("\"code\":410");
        assertThat(mvcResult.getResponse().getContentAsString()).contains(DISABLED_MESSAGE);
        verifyNoInteractions(fileService, fileManagementService, rateLimiter);
    }
}
