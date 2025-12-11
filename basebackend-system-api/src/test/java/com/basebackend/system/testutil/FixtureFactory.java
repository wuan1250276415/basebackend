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
        PermissionDTO dto = new PermissionDTO();
        dto.setId(1L);
        dto.setPermissionName("测试权限");
        dto.setPermissionKey("test:permission");
        dto.setApiPath("/api/test");
        dto.setHttpMethod("GET");
        dto.setPermissionType(1);
        dto.setStatus(1);
        dto.setRemark("测试权限备注");
        return dto;
    }

    /**
     * 创建无效的权限DTO（缺少必填字段）
     */
    public PermissionDTO createInvalidPermissionDTO() {
        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionName(""); // 空名称，违反@NotBlank
        dto.setPermissionKey(""); // 空权限标识，违反@NotBlank
        return dto;
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
        PermissionDTO dto = createValidPermissionDTO();
        dto.setPermissionType(1); // 菜单权限
        dto.setPermissionName("菜单权限");
        return dto;
    }

    public PermissionDTO createButtonPermissionDTO() {
        PermissionDTO dto = createValidPermissionDTO();
        dto.setPermissionType(2); // 按钮权限
        dto.setPermissionName("按钮权限");
        return dto;
    }

    public PermissionDTO createApiPermissionDTO() {
        PermissionDTO dto = createValidPermissionDTO();
        dto.setPermissionType(3); // API权限
        dto.setPermissionName("API权限");
        return dto;
    }

    // ========== 部门相关 ==========

    /**
     * 创建有效的部门DTO
     */
    public DeptDTO createValidDeptDTO() {
        DeptDTO dto = new DeptDTO();
        dto.setId(1L);
        dto.setDeptName("测试部门");
        dto.setParentId(0L);
        dto.setOrderNum(1);
        dto.setLeader("负责人");
        dto.setPhone("13800138000");
        dto.setEmail("test@example.com");
        dto.setStatus(1);
        dto.setRemark("测试部门备注");
        return dto;
    }

    /**
     * 创建无效的部门DTO（缺少必填字段）
     */
    public DeptDTO createInvalidDeptDTO() {
        DeptDTO dto = new DeptDTO();
        dto.setDeptName(""); // 空名称，违反@NotBlank
        return dto;
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
        DeptDTO dto = createValidDeptDTO();
        dto.setId(1L);
        dto.setDeptName("总公司");
        dto.setParentId(0L);
        return dto;
    }

    public DeptDTO createChildDeptDTO() {
        DeptDTO dto = createValidDeptDTO();
        dto.setId(2L);
        dto.setDeptName("分公司");
        dto.setParentId(1L);
        return dto;
    }

    // ========== 应用相关 ==========

    /**
     * 创建有效的应用DTO
     */
    public ApplicationDTO createValidApplicationDTO() {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(1L);
        dto.setAppName("测试应用");
        dto.setAppCode("TEST_APP");
        dto.setAppType("web");
        dto.setAppIcon("icon-test");
        dto.setAppUrl("https://test.example.com");
        dto.setStatus(1);
        dto.setOrderNum(1);
        dto.setRemark("测试应用备注");
        return dto;
    }

    /**
     * 创建无效的应用DTO（缺少必填字段）
     */
    public ApplicationDTO createInvalidApplicationDTO() {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setAppName(""); // 空名称，违反@NotBlank
        dto.setAppCode(""); // 空编码，违反@NotBlank
        dto.setAppType(""); // 空类型，违反@NotBlank
        return dto;
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
        ApplicationDTO dto = createValidApplicationDTO();
        dto.setAppType("web");
        dto.setAppName("Web应用");
        return dto;
    }

    public ApplicationDTO createMobileAppDTO() {
        ApplicationDTO dto = createValidApplicationDTO();
        dto.setAppType("mobile");
        dto.setAppName("移动应用");
        dto.setId(2L);
        dto.setAppCode("MOBILE_APP");
        return dto;
    }

    public ApplicationDTO createApiAppDTO() {
        ApplicationDTO dto = createValidApplicationDTO();
        dto.setAppType("api");
        dto.setAppName("API服务");
        dto.setId(3L);
        dto.setAppCode("API_APP");
        return dto;
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
        DictDTO dto = new DictDTO();
        dto.setId(1L);
        dto.setDictName("用户类型");
        dto.setDictType("user_type");
        dto.setStatus(1);
        dto.setRemark("测试字典备注");
        return dto;
    }

    /**
     * 创建有效的字典数据DTO
     */
    public DictDataDTO createValidDictDataDTO() {
        DictDataDTO dto = new DictDataDTO();
        dto.setId(1L);
        dto.setDictType("user_type");
        dto.setDictLabel("管理员");
        dto.setDictValue("admin");
        dto.setDictSort(1);
        dto.setStatus(1);
        dto.setRemark("测试数据备注");
        return dto;
    }

    // ========== 日志相关 ==========

    /**
     * 创建有效的登录日志DTO
     */
    public LoginLogDTO createValidLoginLogDTO() {
        LoginLogDTO dto = new LoginLogDTO();
        dto.setId(1L);
        dto.setUserId(1L);
        dto.setUsername("admin");
        dto.setIpAddress("127.0.0.1");
        dto.setLoginLocation("本地");
        dto.setBrowser("Chrome");
        dto.setOs("Windows 10");
        dto.setStatus(1);
        dto.setMsg("登录成功");
        dto.setLoginTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建有效的操作日志DTO
     */
    public OperationLogDTO createValidOperationLogDTO() {
        OperationLogDTO dto = new OperationLogDTO();
        dto.setId(1L);
        dto.setUserId(1L);
        dto.setUsername("admin");
        dto.setOperation("创建用户");
        dto.setMethod("POST /api/users");
        dto.setParams("{\"username\":\"test\"}");
        dto.setTime(100L);
        dto.setIpAddress("127.0.0.1");
        dto.setLocation("本地");
        dto.setStatus(1);
        dto.setErrorMsg(null);
        dto.setOperationTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 生成唯一的字典类型
     */
    public String generateUniqueDictType() {
        return "dict_type_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
