package com.basebackend.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.common.model.Result;
import com.basebackend.feign.client.DeptFeignClient;
import com.basebackend.feign.dto.dept.DeptBasicDTO;
import com.basebackend.observability.metrics.CustomMetrics;
import com.basebackend.user.dto.UserCreateDTO;
import com.basebackend.user.dto.UserDTO;
import com.basebackend.user.dto.UserQueryDTO;
import com.basebackend.user.entity.SysRole;
import com.basebackend.user.entity.SysUser;
import com.basebackend.user.entity.SysUserRole;
import com.basebackend.user.mapper.SysRoleMapper;
import com.basebackend.user.mapper.SysUserMapper;
import com.basebackend.user.mapper.SysUserRoleMapper;
import com.basebackend.user.util.AuditHelper;
import com.basebackend.user.util.DeptInfoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

/**
 * UserServiceImpl 单元测试
 * 覆盖用户CRUD、角色分配、唯一性校验等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomMetrics customMetrics;

    @Mock
    private AuditHelper auditHelper;

    @Mock
    private DeptInfoHelper deptInfoHelper;

    @Mock
    private DeptFeignClient deptFeignClient;
    @InjectMocks
    private UserServiceImpl userService;

    private SysUser testUser;
    private UserDTO testUserDTO;
    private UserCreateDTO testUserCreateDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试用户实体
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setDeptId(1L);
        testUser.setUserType(1);
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateTime(LocalDateTime.now());

        // 初始化测试用户DTO
        testUserDTO = new UserDTO();
        testUserDTO.setId(1L);
        testUserDTO.setUsername("testuser");
        testUserDTO.setNickname("测试用户");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setPhone("13800138000");
        testUserDTO.setDeptId(1L);
        testUserDTO.setUserType(1);
        testUserDTO.setStatus(1);
        testUserDTO.setRoleIds(Arrays.asList(1L, 2L));

        // 初始化创建用户DTO
        testUserCreateDTO = new UserCreateDTO();
        testUserCreateDTO.setUsername("newuser");
        testUserCreateDTO.setPassword("password123");
        testUserCreateDTO.setNickname("新用户");
        testUserCreateDTO.setEmail("new@example.com");
        testUserCreateDTO.setPhone("13900139000");
        testUserCreateDTO.setDeptId(1L);
        testUserCreateDTO.setUserType(2);
        testUserCreateDTO.setStatus(1);
        testUserCreateDTO.setRoleIds(Arrays.asList(1L));
    }

    // ==================== 查询测试 ====================

    @Nested
    @DisplayName("用户查询测试")
    class QueryTests {

        @Test
        @DisplayName("根据ID查询用户 - 成功")
        void testGetById_Success() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            UserDTO result = userService.getById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals("测试用户", result.getNickname());
            verify(userMapper).selectById(1L);
        }

        @Test
        @DisplayName("根据ID查询用户 - 用户不存在")
        void testGetById_UserNotFound() {
            // Arrange
            when(userMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.getById(999L));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("根据用户名查询用户 - 成功")
        void testGetByUsername_Success() {
            // Arrange
            when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            UserDTO result = userService.getByUsername("testuser");

            // Assert
            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
        }

        @Test
        @DisplayName("根据用户名查询用户 - 用户不存在")
        void testGetByUsername_UserNotFound() {
            // Arrange
            when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.getByUsername("nonexistent"));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("根据手机号查询用户 - 成功")
        void testGetByPhone_Success() {
            // Arrange
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            UserDTO result = userService.getByPhone("13800138000");

            // Assert
            assertNotNull(result);
            assertEquals("13800138000", result.getPhone());
        }

        @Test
        @DisplayName("根据邮箱查询用户 - 成功")
        void testGetByEmail_Success() {
            // Arrange
            when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            UserDTO result = userService.getByEmail("test@example.com");

            // Assert
            assertNotNull(result);
            assertEquals("test@example.com", result.getEmail());
        }

        @Test
        @DisplayName("分页查询用户列表")
        void testPage_Success() {
            // Arrange
            Page<SysUser> userPage = new Page<>(1, 10);
            userPage.setRecords(Arrays.asList(testUser));
            userPage.setTotal(1);
            userPage.setPages(1);

            when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(userPage);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            doNothing().when(customMetrics).recordBusinessOperation(anyString(), anyString());

            UserQueryDTO queryDTO = new UserQueryDTO();
            queryDTO.setUsername("test");

            // Act
            Page<UserDTO> result = userService.page(queryDTO, 1, 10);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.getTotal());
            assertEquals(1, result.getRecords().size());
        }

        @Test
        @DisplayName("批量查询用户")
        void testGetBatchByIds_Success() {
            // Arrange
            List<Long> ids = Arrays.asList(1L, 2L);
            SysUser user2 = new SysUser();
            user2.setId(2L);
            user2.setUsername("user2");
            user2.setNickname("用户2");

            when(userMapper.selectBatchIds(ids)).thenReturn(Arrays.asList(testUser, user2));
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<UserDTO> result = userService.getBatchByIds(ids);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("批量查询用户 - 空列表")
        void testGetBatchByIds_EmptyList() {
            // Act
            List<UserDTO> result = userService.getBatchByIds(Collections.emptyList());

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("根据部门ID查询用户")
        void testGetByDeptId_Success() {
            // Arrange
            when(userMapper.selectUsersByDeptId(1L)).thenReturn(Arrays.asList(testUser));
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            // Act
            List<UserDTO> result = userService.getByDeptId(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("获取所有活跃用户ID - 需要集成测试环境")
        @org.junit.jupiter.api.Disabled("MyBatis-Plus LambdaQueryWrapper需要集成测试环境初始化实体元数据")
        void testGetAllActiveUserIds_Success() {
            // 此测试需要在集成测试环境中运行，因为LambdaQueryWrapper依赖实体类的Lambda缓存
            // Arrange
            SysUser activeUser = new SysUser();
            activeUser.setId(1L);
            doReturn(Arrays.asList(activeUser)).when(userMapper).selectList(any());

            // Act
            List<Long> result = userService.getAllActiveUserIds();

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1L, result.get(0));
        }
    }


    // ==================== 创建测试 ====================

    @Nested
    @DisplayName("用户创建测试")
    class CreateTests {

        @Test
        @DisplayName("创建用户 - 成功")
        void testCreate_Success() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
            when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
                SysUser user = invocation.getArgument(0);
                user.setId(1L);
                return 1;
            });
            when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.create(testUserCreateDTO));

            // Assert
            verify(userMapper).insert(any(SysUser.class));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("创建用户 - 用户名已存在")
        void testCreate_UsernameExists() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.create(testUserCreateDTO));
            assertEquals("用户名已存在", exception.getMessage());
            verify(userMapper, never()).insert(any(SysUser.class));
        }

        @Test
        @DisplayName("创建用户 - 邮箱已存在")
        void testCreate_EmailExists() {
            // Arrange - 用户名唯一，邮箱不唯一
            when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)  // 用户名检查
                    .thenReturn(1L); // 邮箱检查

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.create(testUserCreateDTO));
            assertEquals("邮箱已存在", exception.getMessage());
        }

        @Test
        @DisplayName("创建用户 - 手机号已存在")
        void testCreate_PhoneExists() {
            // Arrange - 用户名和邮箱唯一，手机号不唯一
            when(userMapper.selectCount(any(LambdaQueryWrapper.class)))
                    .thenReturn(0L)  // 用户名检查
                    .thenReturn(0L)  // 邮箱检查
                    .thenReturn(1L); // 手机号检查

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.create(testUserCreateDTO));
            assertEquals("手机号已存在", exception.getMessage());
        }

        @Test
        @DisplayName("创建用户 - 无角色分配")
        void testCreate_WithoutRoles() {
            // Arrange
            testUserCreateDTO.setRoleIds(null);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userMapper.insert(any(SysUser.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.create(testUserCreateDTO));

            // Assert
            verify(userMapper).insert(any(SysUser.class));
            verify(userRoleMapper, never()).insert(any(SysUserRole.class));
        }
    }

    // ==================== 更新测试 ====================

    @Nested
    @DisplayName("用户更新测试")
    class UpdateTests {

        @Test
        @DisplayName("更新用户 - 成功")
        void testUpdate_Success() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.update(testUserDTO));

            // Assert
            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("更新用户 - 用户不存在")
        void testUpdate_UserNotFound() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.update(testUserDTO));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("更新用户 - 用户名冲突")
        void testUpdate_UsernameConflict() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.update(testUserDTO));
            assertEquals("用户名已存在", exception.getMessage());
        }
    }

    // ==================== 删除测试 ====================

    @Nested
    @DisplayName("用户删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除用户 - 成功")
        void testDelete_Success() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.deleteById(1L)).thenReturn(1);
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.delete(1L));

            // Assert
            verify(userMapper).deleteById(1L);
            verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("删除用户 - 用户不存在")
        void testDelete_UserNotFound() {
            // Arrange
            when(userMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.delete(999L));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("批量删除用户")
        void testDeleteBatch_Success() {
            // Arrange
            when(userMapper.selectById(anyLong())).thenReturn(testUser);
            when(userMapper.deleteById(anyLong())).thenReturn(1);
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.deleteBatch(Arrays.asList(1L, 2L)));

            // Assert
            verify(userMapper, times(2)).deleteById(anyLong());
        }
    }

    // ==================== 密码与状态测试 ====================

    @Nested
    @DisplayName("密码与状态管理测试")
    class PasswordAndStatusTests {

        @Test
        @DisplayName("重置密码 - 成功")
        void testResetPassword_Success() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
            when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.resetPassword(1L, "newPassword"));

            // Assert
            verify(passwordEncoder).encode("newPassword");
            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("重置密码 - 用户不存在")
        void testResetPassword_UserNotFound() {
            // Arrange
            when(userMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.resetPassword(999L, "newPassword"));
            assertEquals("用户不存在", exception.getMessage());
        }

        @Test
        @DisplayName("修改用户状态 - 成功")
        void testChangeStatus_Success() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.changeStatus(1L, 0));

            // Assert
            verify(userMapper).updateById(any(SysUser.class));
        }

        @Test
        @DisplayName("修改用户状态 - 用户不存在")
        void testChangeStatus_UserNotFound() {
            // Arrange
            when(userMapper.selectById(999L)).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> userService.changeStatus(999L, 0));
            assertEquals("用户不存在", exception.getMessage());
        }
    }

    // ==================== 角色分配测试 ====================

    @Nested
    @DisplayName("角色分配测试")
    class RoleAssignmentTests {

        @Test
        @DisplayName("分配角色 - 成功")
        void testAssignRoles_Success() {
            // Arrange
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(userRoleMapper.insert(any(SysUserRole.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.assignRoles(1L, Arrays.asList(1L, 2L)));

            // Assert
            verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
            verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
        }

        @Test
        @DisplayName("分配角色 - 空角色列表")
        void testAssignRoles_EmptyRoles() {
            // Arrange
            when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            // Act
            assertDoesNotThrow(() -> userService.assignRoles(1L, Collections.emptyList()));

            // Assert
            verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
            verify(userRoleMapper, never()).insert(any(SysUserRole.class));
        }

        @Test
        @DisplayName("获取用户角色列表")
        void testGetUserRoles_Success() {
            // Arrange
            SysUserRole userRole1 = new SysUserRole();
            userRole1.setUserId(1L);
            userRole1.setRoleId(1L);
            SysUserRole userRole2 = new SysUserRole();
            userRole2.setUserId(1L);
            userRole2.setRoleId(2L);

            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(userRole1, userRole2));

            // Act
            List<Long> result = userService.getUserRoles(1L);

            // Assert
            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains(1L));
            assertTrue(result.contains(2L));
        }
    }

    // ==================== 唯一性校验测试 ====================

    @Nested
    @DisplayName("唯一性校验测试")
    class UniqueCheckTests {

        @Test
        @DisplayName("检查用户名唯一 - 新建时唯一")
        void testCheckUsernameUnique_NewUser_Unique() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Act
            boolean result = userService.checkUsernameUnique("newuser", null);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("检查用户名唯一 - 新建时不唯一")
        void testCheckUsernameUnique_NewUser_NotUnique() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // Act
            boolean result = userService.checkUsernameUnique("existinguser", null);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("检查用户名唯一 - 更新时排除自身")
        void testCheckUsernameUnique_UpdateUser() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Act
            boolean result = userService.checkUsernameUnique("testuser", 1L);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("检查邮箱唯一")
        void testCheckEmailUnique() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Act
            boolean result = userService.checkEmailUnique("new@example.com", null);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("检查手机号唯一")
        void testCheckPhoneUnique() {
            // Arrange
            when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // Act
            boolean result = userService.checkPhoneUnique("13900139000", null);

            // Assert
            assertTrue(result);
        }
    }

    // ==================== 导出测试 ====================

    @Nested
    @DisplayName("导出测试")
    class ExportTests {

        @Test
        @DisplayName("导出用户数据")
        void testExport_Success() {
            // Arrange
            when(userMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(testUser));
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            UserQueryDTO queryDTO = new UserQueryDTO();

            // Act
            List<UserDTO> result = userService.export(queryDTO);

            // Assert
            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== 部门信息获取测试 ====================

    @Nested
    @DisplayName("部门信息获取测试")
    class DeptInfoTests {

        @Test
        @DisplayName("获取用户时包含部门名称")
        void testGetById_WithDeptName() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            DeptBasicDTO deptDTO = new DeptBasicDTO();
            deptDTO.setDeptName("技术部");
            Result<DeptBasicDTO> deptResult = Result.success(deptDTO);
            when(deptFeignClient.getById(1L)).thenReturn(deptResult);

            // Act
            UserDTO result = userService.getById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("技术部", result.getDeptName());
        }

        @Test
        @DisplayName("获取用户时部门服务异常降级")
        void testGetById_DeptServiceError() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());
            when(deptFeignClient.getById(1L)).thenThrow(new RuntimeException("服务不可用"));

            // Act
            UserDTO result = userService.getById(1L);

            // Assert
            assertNotNull(result);
            assertEquals("", result.getDeptName()); // 降级为空字符串
        }
    }

    // ==================== 角色名称获取测试 ====================

    @Nested
    @DisplayName("角色名称获取测试")
    class RoleNameTests {

        @Test
        @DisplayName("获取用户时包含角色名称")
        void testGetById_WithRoleNames() {
            // Arrange
            when(userMapper.selectById(1L)).thenReturn(testUser);

            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(1L);
            userRole.setRoleId(1L);
            when(userRoleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(userRole));

            SysRole role = new SysRole();
            role.setId(1L);
            role.setRoleName("管理员");
            when(roleMapper.selectById(1L)).thenReturn(role);

            // Act
            UserDTO result = userService.getById(1L);

            // Assert
            assertNotNull(result);
            assertNotNull(result.getRoleNames());
            assertEquals(1, result.getRoleNames().size());
            assertEquals("管理员", result.getRoleNames().get(0));
        }
    }
}
