//package com.basebackend.database.security;
//
//import com.basebackend.database.security.annotation.Sensitive;
//import com.basebackend.database.security.annotation.SensitiveType;
//import com.basebackend.database.security.context.PermissionContext;
//import com.basebackend.database.security.service.DataMaskingService;
//import lombok.Data;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.TestPropertySource;
//
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * 权限控制集成测试
// * 验证权限上下文和数据脱敏功能的集成
// */
//@SpringBootTest(classes = {
//    com.basebackend.database.config.DatabaseEnhancedAutoConfiguration.class,
//    com.basebackend.database.security.service.impl.DataMaskingServiceImpl.class
//})
//@TestPropertySource(properties = {
//    "database.enhanced.security.masking.enabled=true"
//})
//class PermissionControlIntegrationTest {
//
//    @Autowired
//    private DataMaskingService dataMaskingService;
//
//    @BeforeEach
//    void setUp() {
//        // 清除权限上下文
//        PermissionContext.clear();
//    }
//
//    @AfterEach
//    void tearDown() {
//        // 清除权限上下文
//        PermissionContext.clear();
//    }
//
//    @Test
//    void testPermissionContextSetAndGet() {
//        // 设置权限
//        Set<String> permissions = Set.of(PermissionContext.VIEW_PHONE, PermissionContext.VIEW_EMAIL);
//        PermissionContext.setPermissions(permissions);
//
//        // 验证权限
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_PHONE));
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_EMAIL));
//        assertFalse(PermissionContext.hasPermission(PermissionContext.VIEW_ID_CARD));
//
//        // 验证获取权限
//        assertEquals(2, PermissionContext.getPermissions().size());
//    }
//
//    @Test
//    void testPermissionContextWithSuperPermission() {
//        // 设置超级权限
//        PermissionContext.setPermissions(Set.of(PermissionContext.VIEW_SENSITIVE_DATA));
//
//        // 验证所有权限都通过
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_PHONE));
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_EMAIL));
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_ID_CARD));
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_BANK_CARD));
//    }
//
//    @Test
//    void testPermissionContextAddAndRemove() {
//        // 初始为空
//        assertTrue(PermissionContext.isEmpty());
//
//        // 添加权限
//        PermissionContext.addPermission(PermissionContext.VIEW_PHONE);
//        assertFalse(PermissionContext.isEmpty());
//        assertTrue(PermissionContext.hasPermission(PermissionContext.VIEW_PHONE));
//
//        // 移除权限
//        PermissionContext.removePermission(PermissionContext.VIEW_PHONE);
//        assertFalse(PermissionContext.hasPermission(PermissionContext.VIEW_PHONE));
//    }
//
//    @Test
//    void testPermissionContextHasAnyPermission() {
//        PermissionContext.setPermissions(Set.of(PermissionContext.VIEW_PHONE));
//
//        // 测试hasAnyPermission
//        assertTrue(PermissionContext.hasAnyPermission(
//            PermissionContext.VIEW_PHONE,
//            PermissionContext.VIEW_EMAIL
//        ));
//
//        assertFalse(PermissionContext.hasAnyPermission(
//            PermissionContext.VIEW_ID_CARD,
//            PermissionContext.VIEW_BANK_CARD
//        ));
//    }
//
//    @Test
//    void testPermissionContextHasAllPermissions() {
//        PermissionContext.setPermissions(Set.of(
//            PermissionContext.VIEW_PHONE,
//            PermissionContext.VIEW_EMAIL
//        ));
//
//        // 测试hasAllPermissions
//        assertTrue(PermissionContext.hasAllPermissions(
//            PermissionContext.VIEW_PHONE,
//            PermissionContext.VIEW_EMAIL
//        ));
//
//        assertFalse(PermissionContext.hasAllPermissions(
//            PermissionContext.VIEW_PHONE,
//            PermissionContext.VIEW_ID_CARD
//        ));
//    }
//
//    @Test
//    void testPermissionContextUserId() {
//        // 设置用户ID
//        PermissionContext.setUserId(12345L);
//        assertEquals(12345L, PermissionContext.getUserId());
//
//        // 清除后应该为null
//        PermissionContext.clear();
//        assertNull(PermissionContext.getUserId());
//    }
//
//    @Test
//    void testDataMaskingWithPermissions() {
//        String phone = "13812345678";
//        String idCard = "110101199001011234";
//
//        // 测试脱敏功能
//        String maskedPhone = dataMaskingService.maskPhone(phone);
//        assertEquals("138****5678", maskedPhone);
//
//        String maskedIdCard = dataMaskingService.maskIdCard(idCard);
//        assertEquals("110101********1234", maskedIdCard);
//    }
//
//    @Test
//    void testPermissionContextClear() {
//        // 设置权限和用户ID
//        PermissionContext.setPermissions(Set.of(PermissionContext.VIEW_PHONE));
//        PermissionContext.setUserId(123L);
//
//        assertFalse(PermissionContext.isEmpty());
//        assertNotNull(PermissionContext.getUserId());
//
//        // 清除
//        PermissionContext.clear();
//
//        assertTrue(PermissionContext.isEmpty());
//        assertNull(PermissionContext.getUserId());
//    }
//
//    /**
//     * 测试用实体类
//     */
//    @Data
//    static class TestUser {
//        private Long id;
//        private String username;
//
//        @Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_PHONE")
//        private String phone;
//
//        @Sensitive(type = SensitiveType.ID_CARD, requiredPermission = "VIEW_ID_CARD")
//        private String idCard;
//
//        @Sensitive(type = SensitiveType.EMAIL, requiredPermission = "VIEW_EMAIL")
//        private String email;
//    }
//}
