package com.basebackend.database.security.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * 加密拦截器
 * 在保存数据时自动加密标记为@Sensitive的字段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptionInterceptor implements InnerInterceptor {
    
    private final EncryptionService encryptionService;
    private final DatabaseEnhancedProperties properties;
    
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        // 只处理INSERT和UPDATE操作
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        if (sqlCommandType != SqlCommandType.INSERT && sqlCommandType != SqlCommandType.UPDATE) {
            return;
        }
        
        // 检查是否启用加密
        if (!properties.getSecurity().getEncryption().isEnabled()) {
            return;
        }
        
        try {
            encryptSensitiveFields(parameter);
        } catch (Exception e) {
            log.error("Failed to encrypt sensitive fields", e);
            // 不抛出异常，避免影响业务操作
        }
    }
    
    /**
     * 加密敏感字段
     */
    private void encryptSensitiveFields(Object parameter) throws IllegalAccessException {
        if (parameter == null) {
            return;
        }
        
        // 处理Map参数（MyBatis Plus的批量操作）
        if (parameter instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameter;
            for (Object value : map.values()) {
                if (value instanceof Collection) {
                    // 批量操作
                    for (Object item : (Collection<?>) value) {
                        encryptObject(item);
                    }
                } else {
                    encryptObject(value);
                }
            }
        } else {
            encryptObject(parameter);
        }
    }
    
    /**
     * 加密对象中的敏感字段
     */
    private void encryptObject(Object obj) throws IllegalAccessException {
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
                log.warn("Field {} is marked as @Sensitive but is not a String type, skipping encryption", 
                        field.getName());
                continue;
            }
            
            field.setAccessible(true);
            String value = (String) field.get(obj);
            
            if (value != null && !value.isEmpty()) {
                // 加密字段值
                String encryptedValue = encryptionService.encrypt(value);
                field.set(obj, encryptedValue);
                
                log.debug("Encrypted field: {}.{}", clazz.getSimpleName(), field.getName());
            }
        }
        
        // 处理父类字段
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            encryptSuperClassFields(obj, superClass);
        }
    }
    
    /**
     * 加密父类中的敏感字段
     */
    private void encryptSuperClassFields(Object obj, Class<?> superClass) throws IllegalAccessException {
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
                String encryptedValue = encryptionService.encrypt(value);
                field.set(obj, encryptedValue);
                
                log.debug("Encrypted parent field: {}.{}", superClass.getSimpleName(), field.getName());
            }
        }
        
        // 递归处理更上层的父类
        Class<?> grandSuperClass = superClass.getSuperclass();
        if (grandSuperClass != null && grandSuperClass != Object.class) {
            encryptSuperClassFields(obj, grandSuperClass);
        }
    }
}
