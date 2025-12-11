package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.DictDTO;
import com.basebackend.system.dto.DictDataDTO;
import com.basebackend.system.entity.SysDict;
import com.basebackend.system.entity.SysDictData;
import com.basebackend.system.mapper.SysDictMapper;
import com.basebackend.system.mapper.SysDictDataMapper;
import com.basebackend.system.service.impl.DictServiceImpl;
import com.basebackend.common.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

/**
 * 字典服务测试
 */
@DisplayName("DictService 字典服务测试")
class DictServiceTest extends BaseServiceTest {

    @Mock
    private SysDictMapper dictMapper;

    @Mock
    private SysDictDataMapper dictDataMapper;

    private DictService dictService;

    @BeforeEach
    void setUp() {
        dictService = new DictServiceImpl(dictMapper, dictDataMapper);
    }

    @Test
    @DisplayName("getDictPage - 应返回字典分页列表")
    void shouldReturnDictPage() {
        // Given
        SysDict dict1 = createSysDict(1L, "用户类型", "user_type", 1);
        given(dictMapper.selectPage(any(), any())).willReturn(createDictPage(Arrays.asList(dict1)));

        // When
        PageResult<DictDTO> result = dictService.getDictPage(1, 10, "用户", "user", 1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDictName()).isEqualTo("用户类型");
        verify(dictMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("getDictById - 应返回指定ID的字典")
    void shouldReturnDictById() {
        // Given
        SysDict dict = createSysDict(1L, "用户类型", "user_type", 1);
        given(dictMapper.selectById(1L)).willReturn(dict);

        // When
        DictDTO result = dictService.getDictById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDictName()).isEqualTo("用户类型");
        assertThat(result.getDictType()).isEqualTo("user_type");
        verify(dictMapper).selectById(1L);
    }

    @Test
    @DisplayName("createDict - 应创建新字典")
    void shouldCreateDict() {
        // Given
        DictDTO dictDTO = fixtures.createValidDictDTO();

        // When
        dictService.createDict(dictDTO);

        // Then
        verify(dictMapper).insert(any(SysDict.class));
    }

    @Test
    @DisplayName("updateDict - 应更新指定字典")
    void shouldUpdateDict() {
        // Given
        DictDTO dictDTO = fixtures.createValidDictDTO();
        dictDTO.setDictName("更新后的字典名称");

        // When
        dictService.updateDict(1L, dictDTO);

        // Then
        verify(dictMapper).updateById(any(SysDict.class));
    }

    @Test
    @DisplayName("deleteDict - 应删除指定字典及其数据")
    void shouldDeleteDict() {
        // Given
        SysDict dict = createSysDict(1L, "用户类型", "user_type", 1);
        given(dictMapper.selectById(1L)).willReturn(dict);

        // When
        dictService.deleteDict(1L);

        // Then
        verify(dictMapper).deleteById(1L);
        verify(dictDataMapper).delete(any());
    }

    @Test
    @DisplayName("getDictDataByType - 应返回指定类型的字典数据")
    void shouldReturnDictDataByType() {
        // Given
        SysDictData data1 = createSysDictData(1L, "user_type", "管理员", "admin", 1, 1);
        given(dictDataMapper.selectList(any())).willReturn(Arrays.asList(data1));

        // When
        List<DictDataDTO> result = dictService.getDictDataByType("user_type");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDictLabel()).isEqualTo("管理员");
        verify(dictDataMapper).selectList(any());
    }

    @Test
    @DisplayName("getDictDataPage - 应返回字典数据分页列表")
    void shouldReturnDictDataPage() {
        // Given
        SysDictData data1 = createSysDictData(1L, "user_type", "管理员", "admin", 1, 1);
        given(dictDataMapper.selectPage(any(), any())).willReturn(createDictDataPage(Arrays.asList(data1)));

        // When
        PageResult<DictDataDTO> result = dictService.getDictDataPage(1, 10, "user_type", "管理", 1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getDictLabel()).isEqualTo("管理员");
        verify(dictDataMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("getDictDataById - 应返回指定ID的字典数据")
    void shouldReturnDictDataById() {
        // Given
        SysDictData dictData = createSysDictData(1L, "user_type", "管理员", "admin", 1, 1);
        given(dictDataMapper.selectById(1L)).willReturn(dictData);

        // When
        DictDataDTO result = dictService.getDictDataById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDictLabel()).isEqualTo("管理员");
        verify(dictDataMapper).selectById(1L);
    }

    @Test
    @DisplayName("createDictData - 应创建新字典数据")
    void shouldCreateDictData() {
        // Given
        DictDataDTO dictDataDTO = fixtures.createValidDictDataDTO();

        // When
        dictService.createDictData(dictDataDTO);

        // Then
        verify(dictDataMapper).insert(any(SysDictData.class));
    }

    @Test
    @DisplayName("updateDictData - 应更新指定字典数据")
    void shouldUpdateDictData() {
        // Given
        DictDataDTO dictDataDTO = fixtures.createValidDictDataDTO();
        dictDataDTO.setDictLabel("更新后的标签");

        // When
        dictService.updateDictData(1L, dictDataDTO);

        // Then
        verify(dictDataMapper).updateById(any(SysDictData.class));
    }

    @Test
    @DisplayName("deleteDictData - 应删除指定字典数据")
    void shouldDeleteDictData() {
        // Given
        SysDictData dictData = createSysDictData(1L, "user_type", "管理员", "admin", 1, 1);
        given(dictDataMapper.selectById(1L)).willReturn(dictData);

        // When
        dictService.deleteDictData(1L);

        // Then
        verify(dictDataMapper).deleteById(1L);
    }

    private SysDict createSysDict(Long id, String name, String type, Integer status) {
        SysDict dict = new SysDict();
        dict.setId(id);
        dict.setDictName(name);
        dict.setDictType(type);
        dict.setStatus(status);
        dict.setDeleted(0);
        return dict;
    }

    private SysDictData createSysDictData(Long id, String dictType, String label, String value, Integer sort, Integer status) {
        SysDictData dictData = new SysDictData();
        dictData.setId(id);
        dictData.setDictType(dictType);
        dictData.setDictLabel(label);
        dictData.setDictValue(value);
        dictData.setDictSort(sort);
        dictData.setStatus(status);
        dictData.setDeleted(0);
        return dictData;
    }

    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDict> createDictPage(List<SysDict> records) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDict> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }

    private com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDictData> createDictDataPage(List<SysDictData> records) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysDictData> page =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(records);
        page.setTotal(records.size());
        return page;
    }
}
