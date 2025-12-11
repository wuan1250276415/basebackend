package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.DeptDTO;
import com.basebackend.system.entity.SysDept;
import com.basebackend.system.mapper.SysDeptMapper;
import com.basebackend.system.service.impl.DeptServiceImpl;
import com.basebackend.system.util.AuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

/**
 * 部门服务测试
 */
@DisplayName("DeptService 部门服务测试")
class DeptServiceTest extends BaseServiceTest {

    @Mock
    private SysDeptMapper deptMapper;
    @Mock
    private  AuditHelper auditHelper;

    private DeptService deptService;

    @BeforeEach
    void setUp() {
        deptService = new DeptServiceImpl(deptMapper,auditHelper);
    }

    @Test
    @DisplayName("getDeptTree - 应返回部门树形结构")
    void shouldReturnDeptTree() {
        // Given
        SysDept parentDept = createSysDept(1L, "总公司", 0L, 1);
        SysDept childDept = createSysDept(2L, "分公司", 1L, 2);
        given(deptMapper.selectDeptTreeList()).willReturn(Arrays.asList(parentDept, childDept));

        // When
        List<DeptDTO> result = deptService.getDeptTree();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeptName()).isEqualTo("总公司");
        verify(deptMapper).selectDeptTreeList();
    }

    private SysDept createSysDept(Long id, String name, Long parentId, Integer status) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptName(name);
        dept.setParentId(parentId);
        dept.setOrderNum(1);
        dept.setStatus(status);
        return dept;
    }
}
