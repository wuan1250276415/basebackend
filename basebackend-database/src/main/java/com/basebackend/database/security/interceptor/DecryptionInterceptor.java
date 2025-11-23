package com.basebackend.database.security.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;

/**
 * 解密拦截器
 * 在查询数据后自动解密标记为@Sensitive的字段
 * 
 * 执行顺序：此拦截器应在PermissionMaskingInterceptor之前执行
 * Note: This is registered as a bean in MyBatisPlusConfig, not auto-scanned
 */
@Slf4j
@RequiredArgsConstructor
@Order(100) // 确保在PermissionMaskingInterceptor之前执行
@Intercepts({
    @Signature(
        type = ResultSetHandler.class,
        method = "handleResultSets",
        args = {Statement.class}
    )
})
public class DecryptionInterceptor implements Interceptor {
    
    private final EncryptionService encryptionService;
    private final DatabaseEnhancedProperties properties;
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行查询
        Object result = invocation.proceed();
        
        // 检查是否启用加密
        if (!properties.getSecurity().getEncryption().isEnabled()) {
            return result;
        }
        
        try {
            // 解密结果集中的敏感字段
            decryptResult(result);
        } catch (Exception e) {
            log.error("Failed to decrypt sensitive fields", e);
            // 不抛出异常，返回原始结果
        }
        
        return result;
    }
    
    /**
     * 解密结果集
     */
    private void decryptResult(Object result) throws IllegalAccessException {
        if (result == null) {
            return;
        }
        
        if (result instanceof List) {
            // 处理列表结果
            List<?> list = (List<?>) result;
            for (Object item : list) {
                decryptObject(item);
            }
        } else {
            // 处理单个对象
            decryptObject(result);
        }
    }
    
    /**
     * 解密对象中的敏感字段
     */
    private void decryptObject(Object obj) throws IllegalAccessException {
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
            // 检查是否有@Sensitive注解
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive == null || !sensitive.encrypt()) {
                continue;
            }
            
            // 只处理String类型的字段
            if (field.getType() != String.class) {
                continue;
            }
            
            field.setAccessible(true);
            String value = (String) field.get(obj);
            
            if (value != null && !value.isEmpty()) {
                // 解密字段值
                String decryptedValue = encryptionService.decrypt(value);
                field.set(obj, decryptedValue);
                
                log.debug("Decrypted field: {}.{}", clazz.getSimpleName(), field.getName());
            }
        }
        
        // 处理父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            decryptSuperClassFields(obj, superClass);
        }
    }
    
    /**
     * 解密父类中的敏感字段
     */
    private void decryptSuperClassFields(Object obj, Class<?> superClass) throws IllegalAccessException {
        Field[] superFields = superClass.getDeclaredFields();
        for (Field field : superFields) {
            Sensitive sensitive = field.getAnnotation(Sensitive.class);
            if (sensitive == null || !sensitive.encrypt()) {
                continue;
            }
            
            if (field.getType() != String.class) {
                continue;
            }
            
            field.setAccessible(true);
            String value = (String) field.get(obj);
            
            if (value != null && !value.isEmpty()) {
                String decryptedValue = encryptionService.decrypt(value);
                field.set(obj, decryptedValue);
                
                log.debug("Decrypted parent field: {}.{}", superClass.getSimpleName(), field.getName());
            }
        }
        
        // 递归处理更上层的父类
        Class<?> grandSuperClass = superClass.getSuperclass();
        if (grandSuperClass != null && grandSuperClass != Object.class) {
            decryptSuperClassFields(obj, grandSuperClass);
        }
    }
}
