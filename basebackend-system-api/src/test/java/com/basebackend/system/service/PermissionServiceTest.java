package com.basebackend.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.system.base.BaseServiceTest;
import com.basebackend.system.dto.PermissionDTO;
import com.basebackend.system.entity.SysPermission;
import com.basebackend.system.mapper.SysPermissionMapper;
import com.basebackend.system.service.impl.PermissionServiceImpl;
import com.basebackend.system.util.AuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;

/**
 * 权限服务测试
 */
@DisplayName("PermissionService 权限服务测试")
class PermissionServiceTest extends BaseServiceTest {

    @Mock
    private SysPermissionMapper permissionMapper;
    @Mock
    private AuditHelper auditHelper;

    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        permissionService = new PermissionServiceImpl(permissionMapper,auditHelper);
    }

    @Test
    @DisplayName("getPermissionList - 应返回权限列表")
    void shouldReturnPermissionList() {
        // Given
        SysPermission permission1 = createSysPermission(1L, "权限1", "perm1");
        given(permissionMapper.selectList(null)).willReturn(Arrays.asList(permission1));

        // When
        var result = permissionService.getPermissionList();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPermissionName()).isEqualTo("权限1");
        verify(permissionMapper).selectList(null);
    }

    private SysPermission createSysPermission(Long id, String name, String key) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setPermissionName(name);
        permission.setPermissionKey(key);
        permission.setPermissionType(1);
        permission.setStatus(1);
        return permission;
    }
}
