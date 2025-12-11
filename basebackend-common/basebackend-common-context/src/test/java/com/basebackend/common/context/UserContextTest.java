package com.basebackend.common.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserContext 单元测试
 */
@DisplayName("UserContext 测试")
class UserContextTest {

    private UserContext userContext;

    @BeforeEach
    void setUp() {
        userContext = UserContext.builder()
                .userId(1L)
                .username("testuser")
                .nickname("Test User")
                .email("test@example.com")
                .phone("13800138000")
                .userType(2) // 普通用户
                .status(1) // 启用
                .roleIds(Arrays.asList(1L, 2L))
                .roles(new HashSet<>(Arrays.asList("user", "editor")))
                .permissions(new HashSet<>(Arrays.asList("system:user:list", "system:user:query", "system:role:*")))
                .build();
    }

    @Nested
    @DisplayName("权限检查测试")
    class PermissionTests {

        @Test
        @DisplayName("hasPermission检查精确权限")
        void shouldCheckExactPermission() {
            assertTrue(userContext.hasPermission("system:user:list"));
            assertTrue(userContext.hasPermission("system:user:query"));
            assertFalse(userContext.hasPermission("system:user:add"));
            assertFalse(userContext.hasPermission("system:unknown:list"));
        }

        @Test
        @DisplayName("hasPermission支持通配符权限")
        void shouldSupportWildcardPermission() {
            UserContext adminContext = UserContext.builder()
                    .permissions(new HashSet<>(Arrays.asList("*:*:*")))
                    .build();

            assertTrue(adminContext.hasPermission("any:permission:here"));
            assertTrue(adminContext.hasPermission("system:user:add"));
        }

        @Test
        @DisplayName("hasPermission支持单星号通配符")
        void shouldSupportSingleAsteriskWildcard() {
            UserContext adminContext = UserContext.builder()
                    .permissions(new HashSet<>(Arrays.asList("*")))
                    .build();

            assertTrue(adminContext.hasPermission("any:permission"));
        }

        @Test
        @DisplayName("空权限列表返回false")
        void shouldReturnFalseForEmptyPermissions() {
            UserContext emptyContext = UserContext.builder().build();

            assertFalse(emptyContext.hasPermission("any:permission"));
        }

        @Test
        @DisplayName("null权限列表返回false")
        void shouldReturnFalseForNullPermissions() {
            UserContext nullContext = UserContext.builder().permissions(null).build();

            assertFalse(nullContext.hasPermission("any:permission"));
        }

        @Test
        @DisplayName("hasAnyPermission检查任一权限")
        void shouldCheckAnyPermission() {
            assertTrue(userContext.hasAnyPermission("system:user:list", "system:user:add"));
            assertTrue(userContext.hasAnyPermission("nonexistent", "system:user:query"));
            assertFalse(userContext.hasAnyPermission("nonexistent1", "nonexistent2"));
        }

        @Test
        @DisplayName("hasAllPermissions检查所有权限")
        void shouldCheckAllPermissions() {
            assertTrue(userContext.hasAllPermissions("system:user:list", "system:user:query"));
            assertFalse(userContext.hasAllPermissions("system:user:list", "system:user:add"));
        }

        @Test
        @DisplayName("hasAllPermissions空参数返回true")
        void hasAllPermissionsShouldReturnTrueForEmpty() {
            assertTrue(userContext.hasAllPermissions());
            assertTrue(userContext.hasAllPermissions((String[]) null));
        }

        @Test
        @DisplayName("hasAnyPermission空参数返回false")
        void hasAnyPermissionShouldReturnFalseForEmpty() {
            assertFalse(userContext.hasAnyPermission());
            assertFalse(userContext.hasAnyPermission((String[]) null));
        }
    }

    @Nested
    @DisplayName("角色检查测试")
    class RoleTests {

        @Test
        @DisplayName("hasRole检查精确角色")
        void shouldCheckExactRole() {
            assertTrue(userContext.hasRole("user"));
            assertTrue(userContext.hasRole("editor"));
            assertFalse(userContext.hasRole("manager"));
        }

        @Test
        @DisplayName("hasRole对admin角色总是返回true")
        void shouldReturnTrueForAdminRole() {
            UserContext adminContext = UserContext.builder()
                    .roles(new HashSet<>(Arrays.asList("admin")))
                    .build();

            assertTrue(adminContext.hasRole("admin"));
            assertTrue(adminContext.hasRole("any_role")); // admin拥有所有角色
        }

        @Test
        @DisplayName("hasRole对super_admin角色总是返回true")
        void shouldReturnTrueForSuperAdminRole() {
            UserContext superAdminContext = UserContext.builder()
                    .roles(new HashSet<>(Arrays.asList("super_admin")))
                    .build();

            assertTrue(superAdminContext.hasRole("super_admin"));
            assertTrue(superAdminContext.hasRole("any_role"));
        }

        @Test
        @DisplayName("空角色列表返回false")
        void shouldReturnFalseForEmptyRoles() {
            UserContext emptyContext = UserContext.builder().build();

            assertFalse(emptyContext.hasRole("any_role"));
        }

        @Test
        @DisplayName("hasAnyRole检查任一角色")
        void shouldCheckAnyRole() {
            assertTrue(userContext.hasAnyRole("user", "manager"));
            assertTrue(userContext.hasAnyRole("nonexistent", "editor"));
            assertFalse(userContext.hasAnyRole("nonexistent1", "nonexistent2"));
        }

        @Test
        @DisplayName("hasAnyRole空参数返回false")
        void hasAnyRoleShouldReturnFalseForEmpty() {
            assertFalse(userContext.hasAnyRole());
            assertFalse(userContext.hasAnyRole((String[]) null));
        }
    }

    @Nested
    @DisplayName("用户状态检查测试")
    class StatusTests {

        @Test
        @DisplayName("isAdmin检查管理员角色")
        void shouldCheckAdminRole() {
            assertFalse(userContext.isAdmin());

            UserContext adminContext = UserContext.builder()
                    .roles(new HashSet<>(Arrays.asList("admin")))
                    .build();
            assertTrue(adminContext.isAdmin());

            UserContext superAdminContext = UserContext.builder()
                    .roles(new HashSet<>(Arrays.asList("super_admin")))
                    .build();
            assertTrue(superAdminContext.isAdmin());
        }

        @Test
        @DisplayName("isSystemUser检查系统用户类型")
        void shouldCheckSystemUserType() {
            assertFalse(userContext.isSystemUser()); // userType = 2

            UserContext sysUser = UserContext.builder().userType(1).build();
            assertTrue(sysUser.isSystemUser());

            UserContext nullType = UserContext.builder().userType(null).build();
            assertFalse(nullType.isSystemUser());
        }

        @Test
        @DisplayName("isEnabled检查启用状态")
        void shouldCheckEnabledStatus() {
            assertTrue(userContext.isEnabled()); // status = 1

            UserContext disabled = UserContext.builder().status(0).build();
            assertFalse(disabled.isEnabled());

            UserContext nullStatus = UserContext.builder().status(null).build();
            assertFalse(nullStatus.isEnabled());
        }
    }

    @Nested
    @DisplayName("Builder模式测试")
    class BuilderTests {

        @Test
        @DisplayName("Builder创建完整对象")
        void builderShouldCreateCompleteObject() {
            UserContext ctx = UserContext.builder()
                    .userId(1L)
                    .username("testuser")
                    .nickname("Test")
                    .email("test@example.com")
                    .phone("13800138000")
                    .avatar("avatar.jpg")
                    .gender(1)
                    .deptId(10L)
                    .deptName("IT Department")
                    .userType(1)
                    .status(1)
                    .ipAddress("192.168.1.1")
                    .requestTime(System.currentTimeMillis())
                    .build();

            assertNotNull(ctx);
            assertEquals(1L, ctx.getUserId());
            assertEquals("testuser", ctx.getUsername());
            assertEquals("Test", ctx.getNickname());
            assertEquals("test@example.com", ctx.getEmail());
            assertEquals("13800138000", ctx.getPhone());
            assertEquals("avatar.jpg", ctx.getAvatar());
            assertEquals(1, ctx.getGender());
            assertEquals(10L, ctx.getDeptId());
            assertEquals("IT Department", ctx.getDeptName());
            assertEquals(1, ctx.getUserType());
            assertEquals(1, ctx.getStatus());
            assertEquals("192.168.1.1", ctx.getIpAddress());
        }

        @Test
        @DisplayName("NoArgsConstructor创建空对象")
        void noArgsConstructorShouldWork() {
            UserContext ctx = new UserContext();
            assertNotNull(ctx);
            assertNull(ctx.getUserId());
        }

        @Test
        @DisplayName("AllArgsConstructor创建对象")
        void allArgsConstructorShouldWork() {
            Set<String> roles = new HashSet<>();
            Set<String> permissions = new HashSet<>();

            UserContext ctx = new UserContext(
                    1L, "user", "nick", "email", "phone",
                    "avatar", 1, 10L, "dept", 1, 1,
                    Arrays.asList(1L), roles, permissions,
                    "127.0.0.1", System.currentTimeMillis());

            assertNotNull(ctx);
            assertEquals(1L, ctx.getUserId());
        }
    }

    @Nested
    @DisplayName("序列化测试")
    class SerializationTests {

        @Test
        @DisplayName("UserContext实现Serializable")
        void shouldBeSerializable() {
            assertTrue(java.io.Serializable.class.isAssignableFrom(UserContext.class));
        }
    }
}
