package com.basebackend.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.basebackend.system.base.BaseWebMvcTest;
import com.basebackend.system.service.DictService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * 字典控制器测试
 */
@DisplayName("DictController 字典控制器测试")
@WebMvcTest(controllers = DictController.class)
class DictControllerTest extends BaseWebMvcTest {

    @MockBean
    private DictService dictService;

    @Test
    @DisplayName("GET /api/system/dicts - 应返回字典分页列表")
    void shouldReturnDictPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/dicts")
                .param("current", "1")
                .param("size", "10"))
            .andExpect(status().isOk());

        verify(dictService).getDictPage(1, 10, null, null, null);
    }

    @Test
    @DisplayName("GET /api/system/dicts/{id} - 应返回指定ID的字典")
    void shouldReturnDictById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/dicts/1"))
            .andExpect(status().isOk());

        verify(dictService).getDictById(1L);
    }

    @Test
    @DisplayName("POST /api/system/dicts - 应创建新字典")
    void shouldCreateDict() throws Exception {
        // Given
        String requestBody = """
            {
                "dictName": "用户类型",
                "dictType": "user_type",
                "status": 1,
                "remark": "测试字典"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/system/dicts")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk());

        verify(dictService).createDict(any());
    }

    @Test
    @DisplayName("PUT /api/system/dicts/{id} - 应更新指定字典")
    void shouldUpdateDict() throws Exception {
        // Given
        String requestBody = """
            {
                "dictName": "更新后的字典名称",
                "dictType": "user_type",
                "status": 1,
                "remark": "更新备注"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/system/dicts/1")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk());

        verify(dictService).updateDict(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/system/dicts/{id} - 应删除指定字典")
    void shouldDeleteDict() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/dicts/1"))
            .andExpect(status().isOk());

        verify(dictService).deleteDict(1L);
    }

    @Test
    @DisplayName("GET /api/system/dicts/data/type/{dictType} - 应返回指定类型的字典数据")
    void shouldReturnDictDataByType() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/dicts/data/type/user_type"))
            .andExpect(status().isOk());

        verify(dictService).getDictDataByType("user_type");
    }

    @Test
    @DisplayName("GET /api/system/dicts/data - 应返回字典数据分页列表")
    void shouldReturnDictDataPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/dicts/data")
                .param("current", "1")
                .param("size", "10"))
            .andExpect(status().isOk());

        verify(dictService).getDictDataPage(1, 10, null, null, null);
    }

    @Test
    @DisplayName("GET /api/system/dicts/data/{id} - 应返回指定ID的字典数据")
    void shouldReturnDictDataById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/system/dicts/data/1"))
            .andExpect(status().isOk());

        verify(dictService).getDictDataById(1L);
    }

    @Test
    @DisplayName("POST /api/system/dicts/data - 应创建新字典数据")
    void shouldCreateDictData() throws Exception {
        // Given
        String requestBody = """
            {
                "dictType": "user_type",
                "dictLabel": "管理员",
                "dictValue": "admin",
                "dictSort": 1,
                "status": 1,
                "remark": "测试数据"
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/system/dicts/data")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk());

        verify(dictService).createDictData(any());
    }

    @Test
    @DisplayName("PUT /api/system/dicts/data/{id} - 应更新指定字典数据")
    void shouldUpdateDictData() throws Exception {
        // Given
        String requestBody = """
            {
                "dictType": "user_type",
                "dictLabel": "更新后的标签",
                "dictValue": "admin",
                "dictSort": 1,
                "status": 1,
                "remark": "更新备注"
            }
            """;

        // When & Then
        mockMvc.perform(put("/api/system/dicts/data/1")
                .contentType("application/json")
                .content(requestBody))
            .andExpect(status().isOk());

        verify(dictService).updateDictData(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/system/dicts/data/{id} - 应删除指定字典数据")
    void shouldDeleteDictData() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/system/dicts/data/1"))
            .andExpect(status().isOk());

        verify(dictService).deleteDictData(1L);
    }

    @Test
    @DisplayName("POST /api/system/dicts/refresh-cache - 应刷新字典缓存")
    void shouldRefreshCache() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/system/dicts/refresh-cache"))
            .andExpect(status().isOk());

        verify(dictService).refreshCache();
    }
}
