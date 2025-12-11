package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.ApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 应用管理控制器测试
 */
@DisplayName("ApplicationController 应用管理控制器测试")
@WebMvcTest(controllers = ApplicationController.class)
class ApplicationControllerTest extends BaseWebMvcTest {

    @MockBean
    private ApplicationService applicationService;

    @Test
    @DisplayName("GET /api/system/application/list - 应返回应用列表")
    void shouldReturnApplicationList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/application/list"))
            .andExpect(status().isOk());

        verify(applicationService).listApplications();
    }
}
