package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.DeptService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;

/**
 * 部门控制器测试
 */
@Disabled("Nacos context loading issues in @WebMvcTest")
@DisplayName("DeptController 部门控制器测试")
@WebMvcTest(controllers = DeptController.class)
class DeptControllerTest extends BaseWebMvcTest {

    @MockitoBean
    private DeptService deptService;

    @Test
    @DisplayName("GET /api/system/depts/tree - 应返回部门树")
    void shouldReturnDeptTree() throws Exception {
        // Given
        // When & Then
        mockMvc.perform(get("/api/system/depts/tree"))
                .andExpect(status().isOk());

        verify(deptService).getDeptTree();
    }
}
