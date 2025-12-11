package com.basebackend.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.user.dto.RoleDTO;
import com.basebackend.user.entity.*;
import com.basebackend.user.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RoleServiceImpl 单元测试
 * 覆盖角色CRUD、权限分配、菜单分配等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色服务测试")
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRolePermissionMapper rolePermissionMapper;

    @Mock
    private SysRoleResourceMapper roleResourceMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysRoleListOperationMapper roleListOperationMapper;

    @Mock
    private SysRoleDataPermissionMapper roleDataPermissionMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private SysRole testRole;
    private RoleDTO testRoleDTO;

    @BeforeEach
    void setUp() {
        testRole = new SysRole();
        testRole.setId(1L);
        testRole.setRoleName("管理员");
        testRole.setRoleKey("admin");
        testRole.setRoleSort(1);
        testRole.setDataScope(1);
        testRole.setStatus(1);
        testRole.setRemark("系统管理员");
        testRole.setCreateTime(LocalDateTime.now());
        testRole.setUpdateTime(LocalDateTime.now());

        testRoleDTO = new RoleDTO();
        testRoleDTO.setId(1L);
        testRoleDTO.setRoleName("管理员");
        testRoleDTO.setRoleKey("admin");
        testRoleDTO.setRoleSort(1);
        testRoleDTO.setDataScope(1);
        testRoleDTO.setStatus(1);
        testRoleDTO.setMenuIds(Arrays.asList(1L, 2L, 3L));
        testRoleDTO.setPermissionIds(Arrays.asList(1L, 2L));
    }

    @Nested
    @DisplayName("角色查询测试")
    class QueryTests {

        @Test
        @DisplayName("根据ID查询角色 - 成功")
        void testGetById_Success() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(roleResourceMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            RoleDTO result = roleService.getById(1L);

            assertNotNull(result);
            assertEquals("管理员", result.getRoleName());
            assertEquals("admin", result.getRoleKey());
        }

        @Test
        @DisplayName("根据ID查询角色 - 角色不存在")
        void testGetById_RoleNotFound() {
            when(roleMapper.selectById(999L)).thenReturn(null);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.getById(999L));
            assertEquals("角色不存在", exception.getMessage());
        }

        @Test
        @DisplayName("分页查询角色列表")
        void testPage_Success() {
            Page<SysRole> rolePage = new Page<>(1, 10);
            rolePage.setRecords(Arrays.asList(testRole));
            rolePage.setTotal(1);
            rolePage.setPages(1);

            when(roleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(rolePage);
            when(roleResourceMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            Page<RoleDTO> result = roleService.page("管理", null, null, 1, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("获取角色树")
        void testGetRoleTree_Success() {
            SysRole parentRole = new SysRole();
            parentRole.setId(1L);
            parentRole.setParentId(0L);
            parentRole.setRoleName("父角色");

            SysRole childRole = new SysRole();
            childRole.setId(2L);
            childRole.setParentId(1L);
            childRole.setRoleName("子角色");

            when(roleMapper.selectRolesByAppId(1L))
                    .thenReturn(Arrays.asList(parentRole, childRole));

            List<SysRole> result = roleService.getRoleTree(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("父角色", result.get(0).getRoleName());
            assertNotNull(result.get(0).getChildren());
            assertEquals(1, result.get(0).getChildren().size());
        }
    }

    @Nested
    @DisplayName("角色创建测试")
    class CreateTests {

        @Test
        @DisplayName("创建角色 - 成功")
        void testCreate_Success() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(roleMapper.insert(any(SysRole.class))).thenAnswer(invocation -> {
                SysRole role = invocation.getArgument(0);
                role.setId(1L);
                return 1;
            });
            when(roleResourceMapper.insert(any(SysRoleResource.class))).thenReturn(1);
            when(rolePermissionMapper.insert(any(SysRolePermission.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.create(testRoleDTO));

            verify(roleMapper).insert(any(SysRole.class));
            verify(roleResourceMapper, times(3)).insert(any(SysRoleResource.class));
            verify(rolePermissionMapper, times(2)).insert(any(SysRolePermission.class));
        }

        @Test
        @DisplayName("创建角色 - 角色名称已存在")
        void testCreate_RoleNameExists() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.create(testRoleDTO));
            assertEquals("角色名称已存在", exception.getMessage());
        }

        @Test
        @DisplayName("创建角色 - 角色标识已存在")
        void testCreate_RoleKeyExists() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)
                    .thenReturn(1L);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.create(testRoleDTO));
            assertEquals("角色标识已存在", exception.getMessage());
        }

        @Test
        @DisplayName("创建角色 - 无菜单和权限")
        void testCreate_WithoutMenusAndPermissions() {
            testRoleDTO.setMenuIds(null);
            testRoleDTO.setPermissionIds(null);
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.create(testRoleDTO));

            verify(roleMapper).insert(any(SysRole.class));
            verify(roleResourceMapper, never()).insert(any(SysRoleResource.class));
            verify(rolePermissionMapper, never()).insert(any(SysRolePermission.class));
        }
    }

    @Nested
    @DisplayName("角色更新测试")
    class UpdateTests {

        @Test
        @DisplayName("更新角色 - 成功")
        void testUpdate_Success() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);
            when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(roleResourceMapper.insert(any(SysRoleResource.class))).thenReturn(1);
            when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(rolePermissionMapper.insert(any(SysRolePermission.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.update(testRoleDTO));

            verify(roleMapper).updateById(any(SysRole.class));
        }

        @Test
        @DisplayName("更新角色 - 角色不存在")
        void testUpdate_RoleNotFound() {
            when(roleMapper.selectById(1L)).thenReturn(null);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.update(testRoleDTO));
            assertEquals("角色不存在", exception.getMessage());
        }

        @Test
        @DisplayName("更新角色 - 角色名称冲突")
        void testUpdate_RoleNameConflict() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.update(testRoleDTO));
            assertEquals("角色名称已存在", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("角色删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除角色 - 成功")
        void testDelete_Success() {
            when(roleMapper.selectById(1L)).thenReturn(testRole);
            when(roleMapper.deleteById(1L)).thenReturn(1);
            when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.delete(1L));

            verify(roleMapper).deleteById(1L);
            verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
            verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("删除角色 - 角色不存在")
        void testDelete_RoleNotFound() {
            when(roleMapper.selectById(999L)).thenReturn(null);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> roleService.delete(999L));
            assertEquals("角色不存在", exception.getMessage());
        }
    }


    @Nested
    @DisplayName("菜单分配测试")
    class MenuAssignmentTests {

        @Test
        @DisplayName("分配菜单 - 成功")
        void testAssignMenus_Success() {
            when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(roleResourceMapper.insert(any(SysRoleResource.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.assignMenus(1L, Arrays.asList(1L, 2L, 3L)));

            verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
            verify(roleResourceMapper, times(3)).insert(any(SysRoleResource.class));
        }

        @Test
        @DisplayName("分配菜单 - 空菜单列表")
        void testAssignMenus_EmptyMenus() {
            when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.assignMenus(1L, Collections.emptyList()));

            verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
            verify(roleResourceMapper, never()).insert(any(SysRoleResource.class));
        }

        @Test
        @DisplayName("获取角色菜单列表")
        void testGetRoleMenus_Success() {
            SysRoleResource resource1 = new SysRoleResource();
            resource1.setRoleId(1L);
            resource1.setResourceId(1L);
            SysRoleResource resource2 = new SysRoleResource();
            resource2.setRoleId(1L);
            resource2.setResourceId(2L);

            when(roleResourceMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(resource1, resource2));

            List<Long> result = roleService.getRoleMenus(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(1L));
            assertTrue(result.contains(2L));
        }
    }

    @Nested
    @DisplayName("权限分配测试")
    class PermissionAssignmentTests {

        @Test
        @DisplayName("分配权限 - 成功")
        void testAssignPermissions_Success() {
            when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(rolePermissionMapper.insert(any(SysRolePermission.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.assignPermissions(1L, Arrays.asList(1L, 2L)));

            verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
            verify(rolePermissionMapper, times(2)).insert(any(SysRolePermission.class));
        }

        @Test
        @DisplayName("分配权限 - 空权限列表")
        void testAssignPermissions_EmptyPermissions() {
            when(rolePermissionMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.assignPermissions(1L, Collections.emptyList()));

            verify(rolePermissionMapper).delete(any(LambdaQueryWrapper.class));
            verify(rolePermissionMapper, never()).insert(any(SysRolePermission.class));
        }

        @Test
        @DisplayName("获取角色权限列表")
        void testGetRolePermissions_Success() {
            SysRolePermission perm1 = new SysRolePermission();
            perm1.setRoleId(1L);
            perm1.setPermissionId(1L);
            SysRolePermission perm2 = new SysRolePermission();
            perm2.setRoleId(1L);
            perm2.setPermissionId(2L);

            when(rolePermissionMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(perm1, perm2));

            List<Long> result = roleService.getRolePermissions(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("资源分配测试")
    class ResourceAssignmentTests {

        @Test
        @DisplayName("分配资源 - 成功")
        void testAssignResources_Success() {
            when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(roleResourceMapper.insert(any(SysRoleResource.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.assignResources(1L, Arrays.asList(1L, 2L)));

            verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
            verify(roleResourceMapper, times(2)).insert(any(SysRoleResource.class));
        }

        @Test
        @DisplayName("获取角色资源列表")
        void testGetRoleResources_Success() {
            when(roleMapper.selectResourceIdsByRoleId(1L))
                    .thenReturn(Arrays.asList(1L, 2L, 3L));

            List<Long> result = roleService.getRoleResources(1L);

            assertNotNull(result);
            assertEquals(3, result.size());
        }
    }

    @Nested
    @DisplayName("列表操作权限测试")
    class ListOperationTests {

        @Test
        @DisplayName("配置列表操作权限 - 成功")
        void testConfigureListOperations_Success() {
            when(roleListOperationMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(roleListOperationMapper.insert(any(SysRoleListOperation.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                    roleService.configureListOperations(1L, "user", Arrays.asList(1L, 2L)));

            verify(roleListOperationMapper).delete(any(LambdaQueryWrapper.class));
            verify(roleListOperationMapper, times(2)).insert(any(SysRoleListOperation.class));
        }

        @Test
        @DisplayName("获取角色列表操作权限")
        void testGetRoleListOperations_Success() {
            when(roleListOperationMapper.selectOperationIdsByRoleIdAndResourceType(1L, "user"))
                    .thenReturn(Arrays.asList(1L, 2L));

            List<Long> result = roleService.getRoleListOperations(1L, "user");

            assertNotNull(result);
            assertEquals(2, result.size());
        }
    }

    @Nested
    @DisplayName("数据权限测试")
    class DataPermissionTests {

        @Test
        @DisplayName("配置数据权限 - 新建")
        void testConfigureDataPermissions_Create() {
            when(roleDataPermissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(roleDataPermissionMapper.insert(any(SysRoleDataPermission.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                    roleService.configureDataPermissions(1L, "dept_id = 1"));

            verify(roleDataPermissionMapper).insert(any(SysRoleDataPermission.class));
        }

        @Test
        @DisplayName("配置数据权限 - 更新")
        void testConfigureDataPermissions_Update() {
            SysRoleDataPermission existing = new SysRoleDataPermission();
            existing.setId(1L);
            existing.setRoleId(1L);
            existing.setFilterRule("old_rule");

            when(roleDataPermissionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
            when(roleDataPermissionMapper.updateById(any(SysRoleDataPermission.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                    roleService.configureDataPermissions(1L, "dept_id = 2"));

            verify(roleDataPermissionMapper).updateById(any(SysRoleDataPermission.class));
        }
    }

    @Nested
    @DisplayName("角色用户关联测试")
    class RoleUserTests {

        @Test
        @DisplayName("获取角色用户列表")
        void testGetRoleUsers_Success() {
            SysUser user = new SysUser();
            user.setId(1L);
            user.setUsername("testuser");

            when(roleMapper.selectUsersByRoleId(1L, null))
                    .thenReturn(Arrays.asList(user));

            List<SysUser> result = roleService.getRoleUsers(1L, null);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("testuser", result.get(0).getUsername());
        }

        @Test
        @DisplayName("批量关联用户到角色")
        void testAssignUsersToRole_Success() {
            when(userRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                    roleService.assignUsersToRole(1L, Arrays.asList(1L, 2L)));

            verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
        }

        @Test
        @DisplayName("批量关联用户到角色 - 跳过已存在关联")
        void testAssignUsersToRole_SkipExisting() {
            when(userRoleMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(1L)
                    .thenReturn(0L);
            when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            assertDoesNotThrow(() -> 
                    roleService.assignUsersToRole(1L, Arrays.asList(1L, 2L)));

            verify(userRoleMapper, times(1)).insert(any(SysUserRole.class));
        }

        @Test
        @DisplayName("取消用户角色关联")
        void testRemoveUserFromRole_Success() {
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> roleService.removeUserFromRole(1L, 1L));

            verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("唯一性校验测试")
    class UniqueCheckTests {

        @Test
        @DisplayName("检查角色名称唯一 - 新建时唯一")
        void testCheckRoleNameUnique_NewRole_Unique() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            boolean result = roleService.checkRoleNameUnique("新角色", null);

            assertTrue(result);
        }

        @Test
        @DisplayName("检查角色名称唯一 - 新建时不唯一")
        void testCheckRoleNameUnique_NewRole_NotUnique() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            boolean result = roleService.checkRoleNameUnique("已存在角色", null);

            assertFalse(result);
        }

        @Test
        @DisplayName("检查角色标识唯一")
        void testCheckRoleKeyUnique() {
            when(roleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            boolean result = roleService.checkRoleKeyUnique("new_role_key", null);

            assertTrue(result);
        }
    }
}
