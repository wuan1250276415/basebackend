package com.basebackend.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserContextHolder + UserContext 单元测试
 */
class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // ========== 基本操作 ==========

    @Nested
    @DisplayName("基本操作")
    class BasicOperations {

        @Test
        @DisplayName("set/get 正常存取")
        void shouldSetAndGet() {
            var ctx = UserContext.builder().userId(1L).username("admin").build();
            UserContextHolder.set(ctx);
            assertThat(UserContextHolder.get()).isSameAs(ctx);
        }

        @Test
        @DisplayName("clear 后 get 返回 null")
        void shouldReturnNullAfterClear() {
            UserContextHolder.set(UserContext.builder().userId(1L).build());
            UserContextHolder.clear();
            assertThat(UserContextHolder.get()).isNull();
        }

        @Test
        @DisplayName("isPresent 判断上下文是否存在")
        void shouldCheckPresence() {
            assertThat(UserContextHolder.isPresent()).isFalse();
            UserContextHolder.set(UserContext.builder().userId(1L).build());
            assertThat(UserContextHolder.isPresent()).isTrue();
        }

        @Test
        @DisplayName("getOptional 包装")
        void shouldReturnOptional() {
            assertThat(UserContextHolder.getOptional()).isEmpty();
            UserContextHolder.set(UserContext.builder().userId(1L).build());
            assertThat(UserContextHolder.getOptional()).isPresent();
        }

        @Test
        @DisplayName("getOrDefault 返回默认值")
        void shouldReturnDefault() {
            var fallback = UserContext.builder().userId(99L).build();
            assertThat(UserContextHolder.getOrDefault(fallback).getUserId()).isEqualTo(99L);

            UserContextHolder.set(UserContext.builder().userId(1L).build());
            assertThat(UserContextHolder.getOrDefault(fallback).getUserId()).isEqualTo(1L);
        }
    }

    // ========== 便捷访问器 ==========

    @Nested
    @DisplayName("便捷访问器")
    class Accessors {

        @Test
        @DisplayName("getUserId / getUsername / getNickname / getDeptId")
        void shouldAccessFields() {
            var ctx = UserContext.builder()
                    .userId(100L).username("zhangsan").nickname("张三").deptId(10L)
                    .build();
            UserContextHolder.set(ctx);

            assertThat(UserContextHolder.getUserId()).isEqualTo(100L);
            assertThat(UserContextHolder.getUsername()).isEqualTo("zhangsan");
            assertThat(UserContextHolder.getNickname()).isEqualTo("张三");
            assertThat(UserContextHolder.getDeptId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("上下文不存在时访问器返回 null")
        void shouldReturnNullWhenNoContext() {
            assertThat(UserContextHolder.getUserId()).isNull();
            assertThat(UserContextHolder.getUsername()).isNull();
            assertThat(UserContextHolder.getNickname()).isNull();
            assertThat(UserContextHolder.getDeptId()).isNull();
        }

        @Test
        @DisplayName("hasRole / hasPermission 上下文不存在时返回 false")
        void shouldReturnFalseWhenNoContext() {
            assertThat(UserContextHolder.hasRole("admin")).isFalse();
            assertThat(UserContextHolder.hasPermission("sys:user:list")).isFalse();
            assertThat(UserContextHolder.isSuperAdmin()).isFalse();
        }
    }

    // ========== require 方法 ==========

    @Nested
    @DisplayName("require 方法")
    class RequireMethods {

        @Test
        @DisplayName("require 上下文存在时返回")
        void shouldReturnWhenPresent() {
            UserContextHolder.set(UserContext.builder().userId(1L).build());
            assertThat(UserContextHolder.require()).isNotNull();
        }

        @Test
        @DisplayName("require 上下文不存在时抛异常")
        void shouldThrowWhenAbsent() {
            assertThatThrownBy(UserContextHolder::require)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not present");
        }

        @Test
        @DisplayName("requireUserId 正常获取")
        void shouldRequireUserId() {
            UserContextHolder.set(UserContext.builder().userId(42L).build());
            assertThat(UserContextHolder.requireUserId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("requireUserId userId 为 null 时抛异常")
        void shouldThrowWhenUserIdNull() {
            UserContextHolder.set(UserContext.builder().username("test").build());
            assertThatThrownBy(UserContextHolder::requireUserId)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ========== UserContext 业务方法 ==========

    @Nested
    @DisplayName("UserContext 权限/角色")
    class UserContextMethods {

        @Test
        @DisplayName("hasPermission 支持通配符")
        void shouldSupportWildcard() {
            var ctx = UserContext.builder().permissions(Set.of("*:*:*")).build();
            assertThat(ctx.hasPermission("sys:user:add")).isTrue();
            assertThat(ctx.hasPermission("any:thing")).isTrue();
        }

        @Test
        @DisplayName("hasPermission 精确匹配")
        void shouldMatchExact() {
            var ctx = UserContext.builder().permissions(Set.of("sys:user:list", "sys:user:add")).build();
            assertThat(ctx.hasPermission("sys:user:list")).isTrue();
            assertThat(ctx.hasPermission("sys:user:delete")).isFalse();
        }

        @Test
        @DisplayName("hasPermission permissions 为 null 返回 false")
        void shouldReturnFalseForNullPermissions() {
            var ctx = UserContext.builder().build();
            assertThat(ctx.hasPermission("sys:user:list")).isFalse();
        }

        @Test
        @DisplayName("hasAnyPermission 任一匹配")
        void shouldMatchAnyPermission() {
            var ctx = UserContext.builder().permissions(Set.of("sys:user:list")).build();
            assertThat(ctx.hasAnyPermission("sys:user:list", "sys:role:list")).isTrue();
            assertThat(ctx.hasAnyPermission("sys:role:list", "sys:dept:list")).isFalse();
        }

        @Test
        @DisplayName("hasAllPermissions 全部匹配")
        void shouldMatchAllPermissions() {
            var ctx = UserContext.builder().permissions(Set.of("sys:user:list", "sys:user:add")).build();
            assertThat(ctx.hasAllPermissions("sys:user:list", "sys:user:add")).isTrue();
            assertThat(ctx.hasAllPermissions("sys:user:list", "sys:user:delete")).isFalse();
        }

        @Test
        @DisplayName("hasRole admin 角色拥有所有角色")
        void shouldGrantAllRolesToAdmin() {
            var ctx = UserContext.builder().roles(Set.of("admin")).build();
            assertThat(ctx.hasRole("admin")).isTrue();
            assertThat(ctx.hasRole("anything")).isTrue();
            assertThat(ctx.isAdmin()).isTrue();
        }

        @Test
        @DisplayName("hasRole 普通角色精确匹配")
        void shouldMatchExactRole() {
            var ctx = UserContext.builder().roles(Set.of("user", "editor")).build();
            assertThat(ctx.hasRole("user")).isTrue();
            assertThat(ctx.hasRole("admin")).isFalse();
            assertThat(ctx.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("hasAnyRole 任一匹配")
        void shouldMatchAnyRole() {
            var ctx = UserContext.builder().roles(Set.of("editor")).build();
            assertThat(ctx.hasAnyRole("editor", "viewer")).isTrue();
            assertThat(ctx.hasAnyRole("admin_only")).isFalse();
        }

        @Test
        @DisplayName("isSystemUser / isEnabled")
        void shouldCheckUserTypeAndStatus() {
            var ctx = UserContext.builder().userType(1).status(1).build();
            assertThat(ctx.isSystemUser()).isTrue();
            assertThat(ctx.isEnabled()).isTrue();

            var ctx2 = UserContext.builder().userType(2).status(0).build();
            assertThat(ctx2.isSystemUser()).isFalse();
            assertThat(ctx2.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("userType/status 为 null 时返回 false")
        void shouldReturnFalseForNullTypeAndStatus() {
            var ctx = UserContext.builder().build();
            assertThat(ctx.isSystemUser()).isFalse();
            assertThat(ctx.isEnabled()).isFalse();
        }
    }
}
