package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.ApplicationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 应用管理控制器测试
 */
@Disabled("Nacos context loading issues in @WebMvcTest")
@DisplayName("ApplicationController 应用管理控制器测试")
@WebMvcTest(controllers = ApplicationController.class)
class ApplicationControllerTest extends BaseWebMvcTest {

    @MockitoBean
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
