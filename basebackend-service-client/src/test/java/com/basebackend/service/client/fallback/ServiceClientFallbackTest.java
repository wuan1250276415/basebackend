package com.basebackend.service.client.fallback;

import com.basebackend.common.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service Client Fallback 单元测试
 * 验证所有降级实现返回正确的失败/降级结果
 */
class ServiceClientFallbackTest {

    // ========== UserServiceClientFallback ==========

    @Nested
    @DisplayName("UserServiceClientFallback")
    class UserFallbackTest {

        private final UserServiceClientFallback fallback = new UserServiceClientFallback();

        @Test
        @DisplayName("getById 降级返回错误 Result")
        void getByIdShouldReturnError() {
            var result = fallback.getById(1L);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getByUsername 降级返回错误 Result")
        void getByUsernameShouldReturnError() {
            var result = fallback.getByUsername("admin");
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getByPhone 降级返回错误 Result")
        void getByPhoneShouldReturnError() {
            var result = fallback.getByPhone("13800138000");
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getByEmail 降级返回错误 Result")
        void getByEmailShouldReturnError() {
            var result = fallback.getByEmail("test@test.com");
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getBatchByIds 降级返回空列表")
        void getBatchByIdsShouldReturnEmptyList() {
            var result = fallback.getBatchByIds("1,2,3");
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getByDeptId 降级返回空列表")
        void getByDeptIdShouldReturnEmptyList() {
            var result = fallback.getByDeptId(100L);
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getUserRoles 降级返回空列表")
        void getUserRolesShouldReturnEmptyList() {
            var result = fallback.getUserRoles(1L);
            assertThat(result).isNotNull();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("checkUsernameUnique 降级返回 false")
        void checkUsernameUniqueShouldReturnFalse() {
            var result = fallback.checkUsernameUnique("admin", 1L);
            assertThat(result).isNotNull();
            assertThat(result.getData()).isFalse();
        }

        @Test
        @DisplayName("checkEmailUnique 降级返回 false")
        void checkEmailUniqueShouldReturnFalse() {
            var result = fallback.checkEmailUnique("a@b.com", 1L);
            assertThat(result.getData()).isFalse();
        }

        @Test
        @DisplayName("checkPhoneUnique 降级返回 false")
        void checkPhoneUniqueShouldReturnFalse() {
            var result = fallback.checkPhoneUnique("13800000000", 1L);
            assertThat(result.getData()).isFalse();
        }

        @Test
        @DisplayName("getAllActiveUserIds 降级返回空列表")
        void getAllActiveUserIdsShouldReturnEmptyList() {
            var result = fallback.getAllActiveUserIds();
            assertThat(result.getData()).isEmpty();
        }
    }

    // ========== DeptServiceClientFallback ==========

    @Nested
    @DisplayName("DeptServiceClientFallback")
    class DeptFallbackTest {

        private final DeptServiceClientFallback fallback = new DeptServiceClientFallback();

        @Test
        @DisplayName("getById 降级返回错误 Result")
        void getByIdShouldReturnError() {
            var result = fallback.getById(1L);
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getDeptTree 降级返回空列表")
        void getDeptTreeShouldReturnEmptyList() {
            var result = fallback.getDeptTree();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getDeptList 降级返回空列表")
        void getDeptListShouldReturnEmptyList() {
            var result = fallback.getDeptList();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getChildrenByDeptId 降级返回空列表")
        void getChildrenByDeptIdShouldReturnEmptyList() {
            var result = fallback.getChildrenByDeptId(1L);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getChildrenDeptIds 降级返回空列表")
        void getChildrenDeptIdsShouldReturnEmptyList() {
            var result = fallback.getChildrenDeptIds(1L);
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("getByDeptName 降级返回错误")
        void getByDeptNameShouldReturnError() {
            var result = fallback.getByDeptName("技术部");
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getByDeptCode 降级返回错误")
        void getByDeptCodeShouldReturnError() {
            var result = fallback.getByDeptCode("TECH");
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getBatchByIds 降级返回空列表")
        void getBatchByIdsShouldReturnEmptyList() {
            var result = fallback.getBatchByIds("1,2");
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("checkDeptNameUnique 降级返回 false")
        void checkDeptNameUniqueShouldReturnFalse() {
            var result = fallback.checkDeptNameUnique("技术部", 0L, 1L);
            assertThat(result.getData()).isFalse();
        }
    }

    // ========== OperationLogServiceClientFallback ==========

    @Nested
    @DisplayName("OperationLogServiceClientFallback")
    class OperationLogFallbackTest {

        private final OperationLogServiceClientFallback fallback = new OperationLogServiceClientFallback();

        @Test
        @DisplayName("getUserOperationLogs 降级返回空列表")
        void getUserOperationLogsShouldReturnEmptyList() {
            var result = fallback.getUserOperationLogs(1L, 10);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }

        @Test
        @DisplayName("saveOperationLog 降级返回成功（忽略写入）")
        void saveOperationLogShouldReturnSuccess() {
            var result = fallback.saveOperationLog(null);
            assertThat(result.isSuccess()).isTrue();
        }
    }

    // ========== SysRoleResourceServiceClientFallback ==========

    @Nested
    @DisplayName("SysRoleResourceServiceClientFallback")
    class RoleResourceFallbackTest {

        private final SysRoleResourceServiceClientFallback fallback = new SysRoleResourceServiceClientFallback();

        @Test
        @DisplayName("assignMenus 降级返回错误")
        void assignMenusShouldReturnError() {
            var result = fallback.assignMenus(1L, List.of(1L, 2L));
            assertThat(result.isSuccess()).isFalse();
        }

        @Test
        @DisplayName("getRoleMenus 降级返回空列表")
        void getRoleMenusShouldReturnEmptyList() {
            var result = fallback.getRoleMenus(1L);
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isEmpty();
        }
    }
}
