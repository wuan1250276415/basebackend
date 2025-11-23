package com.basebackend.database.security.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.context.PermissionContext;
import com.basebackend.database.security.service.DataMaskingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;

/**
 * 权限脱敏拦截器
 * 在查询数据后，根据用户权限对敏感字段进行脱敏处理
 * 
 * 执行顺序：
 * 1. DecryptionInterceptor 先解密数据
 * 2. PermissionMaskingInterceptor 再根据权限决定是否脱敏
 * 
 * 注意：此拦截器的Order值应大于DecryptionInterceptor，确保在解密之后执行
 * Note: This is registered as a bean in MyBatisPlusConfig, not auto-scanned
 */
@Slf4j
@RequiredArgsConstructor
@Order(200) // DecryptionInterceptor的Order是100，确保在解密之后执行
@Intercepts({
    @Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
    )
})
public class PermissionMaskingInterceptor implements Interceptor {
    
    private final DataMaskingService dataMaskingService;
    private final DatabaseEnhancedProperties properties;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行查询（此时数据已经被DecryptionInterceptor解密）
        Object result = invocation.proceed();
        
        // 检查是否启用脱敏
        if (!properties.getSecurity().getMasking().isEnabled()) {
            return result;
        }
        
        try {
            // 根据权限对结果集中的敏感字段进行脱敏
            maskResultByPermission(result);
        } catch (Exception e) {
            log.error("Failed to mask sensitive fields by permission", e);
            // 不抛出异常，返回原始结果
        }
        
        return result;
    }
    
    /**
     * 根据权限脱敏结果集
     */
    private void maskResultByPermission(Object result) throws IllegalAccessException {
        if (result == null) {
            return;
        }
        
        if (result instanceof List) {
            // 处理列表结果
            List<?> list = (List<?>) result;
            for (Object item : list) {
                maskObjectByPermission(item);
            }
        } else {
            // 处理单个对象
            maskObjectByPermission(result);
        }
    }
    
    /**
     * 根据权限脱敏对象中的敏感字段
     */
    private void maskObjectByPermission(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return;
        }
        
        Class<?> clazz = obj.getClass();
        
        // 跳过基本类型和包装类
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            return;
        }
        
        // 遍历所有字段
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            maskFieldByPermission(obj, field);
        }
        
        // 处理父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            maskSuperClassFieldsByPermission(obj, superClass);
        }
    }
    
    /**
     * 根据权限脱敏字段
     */
    private void maskFieldByPermission(Object obj, Field field) throws IllegalAccessException {
        // 检查是否有@Sensitive注解
        Sensitive sensitive = field.getAnnotation(Sensitive.class);
        if (sensitive == null || !sensitive.mask()) {
            return;
        }
        
        // 只处理String类型的字段
        if (field.getType() != String.class) {
            return;
        }
        
        // 获取所需权限
        String requiredPermission = getRequiredPermission(sensitive);
        
        // 检查用户是否拥有查看权限
        if (PermissionContext.hasPermission(requiredPermission)) {
            // 用户有权限，不脱敏
            log.debug("User has permission '{}' to view field: {}.{}", 
                requiredPermission, obj.getClass().getSimpleName(), field.getName());
            return;
        }
        
        // 用户没有权限，进行脱敏
        field.setAccessible(true);
        String value = (String) field.get(obj);
        
        if (StringUtils.hasText(value)) {
            // 根据敏感类型进行脱敏
            String maskedValue = dataMaskingService.mask(value, sensitive.type());
            field.set(obj, maskedValue);
            
            log.debug("Masked field by permission: {}.{}, type: {}, required permission: {}", 
                obj.getClass().getSimpleName(), field.getName(), sensitive.type(), requiredPermission);
        }
    }
    
    /**
     * 根据权限脱敏父类中的敏感字段
     */
    private void maskSuperClassFieldsByPermission(Object obj, Class<?> superClass) throws IllegalAccessException {
        Field[] superFields = superClass.getDeclaredFields();
        for (Field field : superFields) {
            maskFieldByPermission(obj, field);
        }
        
        // 递归处理更上层的父类
        Class<?> grandSuperClass = superClass.getSuperclass();
        if (grandSuperClass != null && grandSuperClass != Object.class) {
            maskSuperClassFieldsByPermission(obj, grandSuperClass);
        }
    }
    
    /**
     * 获取字段所需的权限
     * 如果注解中指定了权限，则使用指定的权限
     * 否则根据敏感类型返回默认权限
     */
    private String getRequiredPermission(Sensitive sensitive) {
        // 如果注解中指定了权限，直接使用
        if (StringUtils.hasText(sensitive.requiredPermission())) {
            return sensitive.requiredPermission();
        }
        
        // 根据敏感类型返回默认权限
        return switch (sensitive.type()) {
            case PHONE -> PermissionContext.VIEW_PHONE;
            case ID_CARD -> PermissionContext.VIEW_ID_CARD;
            case BANK_CARD -> PermissionContext.VIEW_BANK_CARD;
            case EMAIL -> PermissionContext.VIEW_EMAIL;
            case ADDRESS -> PermissionContext.VIEW_ADDRESS;
            case PASSWORD, CUSTOM -> PermissionContext.VIEW_SENSITIVE_DATA;
        };
    }
}
