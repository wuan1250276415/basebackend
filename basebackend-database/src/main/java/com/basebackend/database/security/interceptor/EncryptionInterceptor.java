package com.basebackend.database.security.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.exception.EncryptionException;
import com.basebackend.database.security.annotation.Sensitive;
import com.basebackend.database.security.service.AlertService;
import com.basebackend.database.security.service.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 加密拦截器
 * 在保存数据时自动加密标记为@Sensitive的字段
 * 支持严格模式：加密失败时阻止业务操作
 */
@Slf4j
public class EncryptionInterceptor implements InnerInterceptor {

    private final EncryptionService encryptionService;
    private final AlertService alertService;
    private final DatabaseEnhancedProperties properties;

    // Performance monitoring
    private static final AtomicLong TOTAL_ENCRYPTION_OPERATIONS = new AtomicLong(0);
    private static final AtomicLong FAILED_ENCRYPTION_OPERATIONS = new AtomicLong(0);
    private static final AtomicLong TOTAL_FIELDS_ENCRYPTED = new AtomicLong(0);

    // Field cache for sensitive fields (class -> list of sensitive fields)
    private static final ConcurrentHashMap<Class<?>, Field[]> SENSITIVE_FIELD_CACHE = new ConcurrentHashMap<>();

    // Operation ID generator
    private static final AtomicLong OPERATION_ID_GENERATOR = new AtomicLong(0);

    public EncryptionInterceptor(EncryptionService encryptionService,
                                  AlertService alertService,
                                  DatabaseEnhancedProperties properties) {
        this.encryptionService = encryptionService;
        this.alertService = alertService;
        this.properties = properties;
    }

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

        long operationId = OPERATION_ID_GENERATOR.incrementAndGet();
        long startTime = System.currentTimeMillis();
        TOTAL_ENCRYPTION_OPERATIONS.incrementAndGet();

        String mapperId = ms.getId();
        log.debug("Encryption operation started: operationId={}, mapperId={}, operation={}",
            operationId, mapperId, sqlCommandType.name());

        try {
            encryptSensitiveFields(parameter, operationId);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Encryption completed successfully: operationId={}, mapperId={}, duration={}ms",
                operationId, mapperId, duration);

        } catch (Exception e) {
            FAILED_ENCRYPTION_OPERATIONS.incrementAndGet();
            long duration = System.currentTimeMillis() - startTime;
            boolean strictMode = properties.getSecurity().getEncryption().isStrictMode();

            // Enhanced error logging with operation context
            log.error("Encryption failed: operationId={}, mapperId={}, duration={}ms, strictMode={}, error={}",
                operationId, mapperId, duration, strictMode, e.getMessage(), e);

            if (strictMode) {
                // 严格模式：加密失败时抛出异常，阻止业务操作
                throw new EncryptionException(
                    String.format("字段加密失败（严格模式），业务操作被阻止！operationId=%d, mapperId=%s",
                        operationId, mapperId), e);
            } else {
                // 非严格模式：发送告警，允许业务操作继续（可能以明文存储）
                try {
                    alertService.sendEncryptionFailureAlert(e);
                    log.warn("Alert sent for encryption failure (non-strict mode): operationId={}", operationId);
                } catch (Exception alertEx) {
                    log.error("Failed to send encryption failure alert: operationId={}, error={}",
                        operationId, alertEx.getMessage(), alertEx);
                }
            }
        }
    }

    /**
     * 加密敏感字段
     */
    private void encryptSensitiveFields(Object parameter, long operationId) throws IllegalAccessException {
        if (parameter == null) {
            log.trace("Encryption skipped: operationId={}, parameter is null", operationId);
            return;
        }

        // 处理Map参数（MyBatis Plus的批量操作）
        if (parameter instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) parameter;
            int encryptedCount = 0;
            for (Object value : map.values()) {
                if (value instanceof Collection) {
                    // 批量操作
                    for (Object item : (Collection<?>) value) {
                        encryptedCount += encryptObject(item, operationId);
                    }
                } else {
                    encryptedCount += encryptObject(value, operationId);
                }
            }
            log.trace("Encryption completed for Map parameter: operationId={}, encryptedFields={}",
                operationId, encryptedCount);
        } else {
            int encryptedCount = encryptObject(parameter, operationId);
            log.trace("Encryption completed for single object: operationId={}, encryptedFields={}",
                operationId, encryptedCount);
        }
    }

    /**
     * 加密对象中的敏感字段
     */
    private int encryptObject(Object obj, long operationId) throws IllegalAccessException {
        if (obj == null) {
            return 0;
        }

        Class<?> clazz = obj.getClass();

        // 跳过基本类型和包装类
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            return 0;
        }

        int encryptedCount = 0;

        // 获取缓存的敏感字段
        Field[] sensitiveFields = getCachedSensitiveFields(clazz);

        // 遍历所有敏感字段
        for (Field field : sensitiveFields) {
            // 只处理String类型的字段
            if (field.getType() != String.class) {
                log.warn("Field {} is marked as @Sensitive but is not a String type, skipping encryption: operationId={}",
                        field.getName(), operationId);
                continue;
            }

            String value = (String) field.get(obj);

            if (value != null && !value.isEmpty()) {
                try {
                    // 加密字段值
                    String encryptedValue = encryptionService.encrypt(value);
                    field.set(obj, encryptedValue);
                    encryptedCount++;
                    TOTAL_FIELDS_ENCRYPTED.incrementAndGet();

                    log.trace("Encrypted field: {}.{}, operationId={}",
                        clazz.getSimpleName(), field.getName(), operationId);
                } catch (Exception e) {
                    log.error("Failed to encrypt field {}.{}: operationId={}, error={}",
                        clazz.getSimpleName(), field.getName(), operationId, e.getMessage(), e);
                    throw e;
                }
            }
        }

        // 递归处理父类敏感字段
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            encryptedCount += encryptSuperClassFields(obj, superClass, operationId);
        }

        return encryptedCount;
    }

    /**
     * 获取缓存的敏感字段
     */
    private Field[] getCachedSensitiveFields(Class<?> clazz) {
        return SENSITIVE_FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> sensitiveFields = new ArrayList<>();
            Class<?> current = clazz;

            // 遍历类层次结构，查找所有@Sensitive注解的字段
            while (current != null && current != Object.class) {
                Field[] declaredFields = current.getDeclaredFields();
                for (Field field : declaredFields) {
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    if (sensitive != null && sensitive.encrypt()) {
                        sensitiveFields.add(field);
                        field.setAccessible(true); // Pre-set accessible for performance
                    }
                }
                current = current.getSuperclass();
            }

            return sensitiveFields.toArray(new Field[0]);
        });
    }

    /**
     * 加密父类中的敏感字段
     */
    private int encryptSuperClassFields(Object obj, Class<?> superClass, long operationId) throws IllegalAccessException {
        if (superClass == null || superClass == Object.class) {
            return 0;
        }

        int encryptedCount = 0;

        // 获取缓存的父类敏感字段
        Field[] sensitiveFields = getCachedSensitiveFields(superClass);

        for (Field field : sensitiveFields) {
            if (field.getType() != String.class) {
                continue;
            }

            String value = (String) field.get(obj);

            if (value != null && !value.isEmpty()) {
                try {
                    String encryptedValue = encryptionService.encrypt(value);
                    field.set(obj, encryptedValue);
                    encryptedCount++;
                    TOTAL_FIELDS_ENCRYPTED.incrementAndGet();

                    log.trace("Encrypted parent field: {}.{}, operationId={}",
                        superClass.getSimpleName(), field.getName(), operationId);
                } catch (Exception e) {
                    log.error("Failed to encrypt parent field {}.{}: operationId={}, error={}",
                        superClass.getSimpleName(), field.getName(), operationId, e.getMessage(), e);
                    throw e;
                }
            }
        }

        // 递归处理更上层的父类
        Class<?> grandSuperClass = superClass.getSuperclass();
        if (grandSuperClass != null && grandSuperClass != Object.class) {
            encryptedCount += encryptSuperClassFields(obj, grandSuperClass, operationId);
        }

        return encryptedCount;
    }

    /**
     * 获取加密性能统计信息
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEncryptionOperations", TOTAL_ENCRYPTION_OPERATIONS.get());
        stats.put("failedEncryptionOperations", FAILED_ENCRYPTION_OPERATIONS.get());
        stats.put("totalFieldsEncrypted", TOTAL_FIELDS_ENCRYPTED.get());
        stats.put("successRate", TOTAL_ENCRYPTION_OPERATIONS.get() > 0 ?
            ((double) (TOTAL_ENCRYPTION_OPERATIONS.get() - FAILED_ENCRYPTION_OPERATIONS.get())
                / TOTAL_ENCRYPTION_OPERATIONS.get()) * 100 : 0);
        stats.put("cachedClassTypes", SENSITIVE_FIELD_CACHE.size());
        return stats;
    }

    /**
     * 重置性能计数器（用于测试）
     */
    public void resetPerformanceCounters() {
        TOTAL_ENCRYPTION_OPERATIONS.set(0);
        FAILED_ENCRYPTION_OPERATIONS.set(0);
        TOTAL_FIELDS_ENCRYPTED.set(0);
        log.info("Encryption interceptor performance counters reset");
    }

    /**
     * 清除字段缓存（用于测试或内存管理）
     */
    public void clearFieldCache() {
        SENSITIVE_FIELD_CACHE.clear();
        log.info("Encryption interceptor field cache cleared");
    }
}
