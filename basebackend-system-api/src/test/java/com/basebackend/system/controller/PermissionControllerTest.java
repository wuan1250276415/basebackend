package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.PermissionService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 权限控制器测试
 */
@Disabled("Nacos context loading issues in @WebMvcTest")
@DisplayName("PermissionController 权限控制器测试")
@WebMvcTest(controllers = PermissionController.class)
class PermissionControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private PermissionService permissionService;

    @Test
    @DisplayName("GET /api/permissions - 应返回权限列表")
    void shouldReturnPermissionList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/permissions"))
                .andExpect(status().isOk());

        verify(permissionService).getPermissionList();
    }
}
