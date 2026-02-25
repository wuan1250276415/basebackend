package com.basebackend.system.testutil;

import com.basebackend.system.dto.ApplicationDTO;
import com.basebackend.system.dto.DeptDTO;
import com.basebackend.system.dto.DictDTO;
import com.basebackend.system.dto.DictDataDTO;
import com.basebackend.system.dto.LoginLogDTO;
import com.basebackend.system.dto.OperationLogDTO;
import com.basebackend.system.dto.PermissionDTO;
import com.basebackend.system.entity.SysApplication;
import com.basebackend.system.entity.SysDept;
import com.basebackend.system.entity.SysPermission;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 测试数据构造器工厂
 * <p>
 * 集中生成合法/非法测试对象，减少重复代码。
 * 所有 DTO 均为 record 类型（不可变），通过全参构造器创建。
 * </p>
 */
public final class FixtureFactory {

    private FixtureFactory() {
    }

    public static FixtureFactory standard() {
        return new FixtureFactory();
    }

    // ========== 权限相关 ==========

    /**
     * 创建有效的权限DTO
     */
    public PermissionDTO createValidPermissionDTO() {
        return new PermissionDTO(1L, "测试权限", "test:permission", "/api/test", "GET", 1, 1, "测试权限备注");
    }

    /**
     * 创建无效的权限DTO（缺少必填字段）
     */
    public PermissionDTO createInvalidPermissionDTO() {
        return new PermissionDTO(null, "", "", "", "", null, null, "");
    }

    /**
     * 创建有效的权限实体
     */
    public SysPermission createValidSysPermission() {
        SysPermission permission = new SysPermission();
        permission.setId(1L);
        permission.setPermissionName("测试权限");
        permission.setPermissionKey("test:permission");
        permission.setApiPath("/api/test");
        permission.setHttpMethod("GET");
        permission.setPermissionType(1);
        permission.setStatus(1);
        permission.setRemark("测试权限备注");
        permission.setCreateTime(LocalDateTime.now());
        permission.setUpdateTime(LocalDateTime.now());
        permission.setCreateBy(1L);
        permission.setUpdateBy(1L);
        return permission;
    }

    /**
     * 创建不同类型的权限DTO
     */
    public PermissionDTO createMenuPermissionDTO() {
        return new PermissionDTO(1L, "菜单权限", "test:permission", "/api/test", "GET", 1, 1, "测试权限备注");
    }

    public PermissionDTO createButtonPermissionDTO() {
        return new PermissionDTO(1L, "按钮权限", "test:permission", "/api/test", "GET", 2, 1, "测试权限备注");
    }

    public PermissionDTO createApiPermissionDTO() {
        return new PermissionDTO(1L, "API权限", "test:permission", "/api/test", "GET", 3, 1, "测试权限备注");
    }

    // ========== 部门相关 ==========

    /**
     * 创建有效的部门DTO
     */
    public DeptDTO createValidDeptDTO() {
        return new DeptDTO(1L, "测试部门", 0L, 1, "负责人", "13800138000", "test@example.com", 1, "测试部门备注", null);
    }

    /**
     * 创建无效的部门DTO（缺少必填字段）
     */
    public DeptDTO createInvalidDeptDTO() {
        return new DeptDTO(null, "", null, null, null, null, null, null, null, null);
    }

    /**
     * 创建有效的部门实体
     */
    public SysDept createValidSysDept() {
        SysDept dept = new SysDept();
        dept.setId(1L);
        dept.setDeptName("测试部门");
        dept.setParentId(0L);
        dept.setOrderNum(1);
        dept.setLeader("负责人");
        dept.setPhone("13800138000");
        dept.setEmail("test@example.com");
        dept.setStatus(1);
        dept.setRemark("测试部门备注");
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        dept.setCreateBy(1L);
        dept.setUpdateBy(1L);
        return dept;
    }

    /**
     * 创建不同层级的部门DTO
     */
    public DeptDTO createParentDeptDTO() {
        return new DeptDTO(1L, "总公司", 0L, 1, "负责人", "13800138000", "test@example.com", 1, "测试部门备注", null);
    }

    public DeptDTO createChildDeptDTO() {
        return new DeptDTO(2L, "分公司", 1L, 1, "负责人", "13800138000", "test@example.com", 1, "测试部门备注", null);
    }

    // ========== 应用相关 ==========

    /**
     * 创建有效的应用DTO
     */
    public ApplicationDTO createValidApplicationDTO() {
        return new ApplicationDTO(1L, "测试应用", "TEST_APP", "web", "icon-test",
                "https://test.example.com", 1, 1, "测试应用备注");
    }

    /**
     * 创建无效的应用DTO（缺少必填字段）
     */
    public ApplicationDTO createInvalidApplicationDTO() {
        return new ApplicationDTO(null, "", "", "", null, null, null, null, null);
    }

    /**
     * 创建有效的应用实体
     */
    public SysApplication createValidSysApplication() {
        SysApplication application = new SysApplication();
        application.setId(1L);
        application.setAppName("测试应用");
        application.setAppCode("TEST_APP");
        application.setAppType("web");
        application.setAppIcon("icon-test");
        application.setAppUrl("https://test.example.com");
        application.setStatus(1);
        application.setOrderNum(1);
        application.setRemark("测试应用备注");
        application.setDeleted(0);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        application.setCreateBy(1L);
        application.setUpdateBy(1L);
        return application;
    }

    /**
     * 创建不同类型的应用DTO
     */
    public ApplicationDTO createWebAppDTO() {
        return new ApplicationDTO(1L, "Web应用", "TEST_APP", "web", "icon-test",
                "https://test.example.com", 1, 1, "测试应用备注");
    }

    public ApplicationDTO createMobileAppDTO() {
        return new ApplicationDTO(2L, "移动应用", "MOBILE_APP", "mobile", "icon-test",
                "https://test.example.com", 1, 1, "测试应用备注");
    }

    public ApplicationDTO createApiAppDTO() {
        return new ApplicationDTO(3L, "API服务", "API_APP", "api", "icon-test",
                "https://test.example.com", 1, 1, "测试应用备注");
    }

    // ========== 通用方法 ==========

    /**
     * 生成唯一的权限标识
     */
    public String generateUniquePermissionKey() {
        return "perm:" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 生成唯一的部门名称
     */
    public String generateUniqueDeptName() {
        return "部门" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 生成唯一的应用编码
     */
    public String generateUniqueAppCode() {
        return "APP_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    // ========== 字典相关 ==========

    /**
     * 创建有效的字典DTO
     */
    public DictDTO createValidDictDTO() {
        return new DictDTO(1L, null, "用户类型", "user_type", 1, "测试字典备注");
    }

    /**
     * 创建有效的字典数据DTO
     */
    public DictDataDTO createValidDictDataDTO() {
        return new DictDataDTO(1L, null, 1, "管理员", "admin", "user_type", null, null, null, 1, "测试数据备注");
    }

    // ========== 日志相关 ==========

    /**
     * 创建有效的登录日志DTO
     */
    public LoginLogDTO createValidLoginLogDTO() {
        return new LoginLogDTO("1", 1L, "admin", "127.0.0.1", "本地", "Chrome", "Windows 10", 1, "登录成功",
                LocalDateTime.now());
    }

    /**
     * 创建有效的操作日志DTO
     */
    public OperationLogDTO createValidOperationLogDTO() {
        return new OperationLogDTO("1", 1L, "admin", "创建用户", "POST /api/users",
                "{\"username\":\"test\"}", 100L, "127.0.0.1", "本地", 1, null, LocalDateTime.now());
    }

    /**
     * 生成唯一的字典类型
     */
    public String generateUniqueDictType() {
        return "dict_type_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
