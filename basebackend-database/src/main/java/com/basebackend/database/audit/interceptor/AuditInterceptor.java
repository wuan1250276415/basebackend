package com.basebackend.database.audit.interceptor;

import com.basebackend.database.audit.entity.AuditLog;
import com.basebackend.database.audit.service.AuditLogService;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 审计拦截器
 * 拦截 INSERT, UPDATE, DELETE 操作，记录审计日志
 * Note: This is registered as a bean in MyBatisPlusConfig, not auto-scanned
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditInterceptor implements Interceptor {

    private AuditLogService auditLogService;
    private DatabaseEnhancedProperties properties;
    private ObjectMapper objectMapper;

    // Thread local to store before data for UPDATE operations
    private static final ThreadLocal<Map<String, Object>> BEFORE_DATA = new ThreadLocal<>();

    public AuditInterceptor(AuditLogService auditLogService, 
                           DatabaseEnhancedProperties properties,
                           ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        // Ensure Java 8 time types (e.g., LocalDateTime) are always supported in audit serialization
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.getAudit().isEnabled()) {
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        SqlCommandType sqlCommandType = ms.getSqlCommandType();
        String mapperId = ms.getId();
        
        // Skip audit log mapper operations to prevent infinite recursion
        if (mapperId != null && mapperId.contains("AuditLogMapper")) {
            return invocation.proceed();
        }
        
        String tableName = extractTableName(mapperId);

        // Skip excluded tables
        if (isExcludedTable(tableName)) {
            return invocation.proceed();
        }

        try {
            // Capture before data for UPDATE operations
            if (sqlCommandType == SqlCommandType.UPDATE) {
                captureBeforeData(parameter);
            }

            // Execute the actual update
            Object result = invocation.proceed();

            // Record audit log after successful execution
            int affectedRows = (Integer) result;
            recordAuditLog(sqlCommandType, tableName, parameter, affectedRows);

            return result;
        } catch (Exception e) {
            log.error("Error in audit interceptor", e);
            throw e;
        } finally {
            BEFORE_DATA.remove();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No additional properties needed
    }

    /**
     * Record audit log
     */
    private void recordAuditLog(SqlCommandType sqlCommandType, String tableName, Object parameter, int result) {
        if (result == 0) {
            return; // No rows affected, skip audit
        }

        try {
            AuditLog auditLog = buildAuditLog(sqlCommandType, tableName, parameter, result);
            
            if (auditLog != null) {
                if (properties.getAudit().isAsync()) {
                    auditLogService.logAsync(auditLog);
                } else {
                    auditLogService.log(auditLog);
                }
            }
        } catch (Exception e) {
            log.error("Failed to record audit log", e);
        }
    }

    /**
     * Capture before data for UPDATE operations
     */
    private void captureBeforeData(Object parameter) {
        if (parameter == null) {
            return;
        }

        try {
            Map<String, Object> beforeData = new HashMap<>();
            
            if (parameter instanceof Map) {
                Map<?, ?> paramMap = (Map<?, ?>) parameter;
                Object entity = paramMap.get("et");
                if (entity != null) {
                    beforeData = extractEntityData(entity);
                }
            } else {
                beforeData = extractEntityData(parameter);
            }
            
            BEFORE_DATA.set(beforeData);
        } catch (Exception e) {
            log.error("Failed to capture before data", e);
        }
    }

    /**
     * Build audit log
     */
    private AuditLog buildAuditLog(SqlCommandType sqlCommandType, String tableName, Object parameter, int result) {
        if (result == 0) {
            return null; // No rows affected, skip audit
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setOperationType(sqlCommandType.name());
        auditLog.setTableName(tableName);
        auditLog.setOperateTime(new Date());

        try {
            Map<String, Object> afterData = extractEntityData(parameter);
            
            // Extract primary key
            String primaryKey = extractPrimaryKey(parameter);
            auditLog.setPrimaryKey(primaryKey);

            // Set after data
            auditLog.setAfterData(objectMapper.writeValueAsString(afterData));

            // For UPDATE operations, set before data and changed fields
            if (sqlCommandType == SqlCommandType.UPDATE) {
                Map<String, Object> beforeData = BEFORE_DATA.get();
                if (beforeData != null && !beforeData.isEmpty()) {
                    auditLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
                    auditLog.setChangedFields(calculateChangedFields(beforeData, afterData));
                }
            }

            // For DELETE operations, store the deleted data in beforeData
            if (sqlCommandType == SqlCommandType.DELETE) {
                auditLog.setBeforeData(objectMapper.writeValueAsString(afterData));
            }

            // Set operator information (would be populated from security context in real implementation)
            // For now, we'll leave these as null or get from thread local if available
            auditLog.setOperatorId(getCurrentUserId());
            auditLog.setOperatorName(getCurrentUserName());
            auditLog.setOperatorIp(getCurrentUserIp());
            auditLog.setTenantId(getCurrentTenantId());

        } catch (Exception e) {
            log.error("Failed to build audit log", e);
            return null;
        }

        return auditLog;
    }

    /**
     * Extract entity data using reflection
     */
    private Map<String, Object> extractEntityData(Object entity) {
        Map<String, Object> data = new HashMap<>();
        
        if (entity == null) {
            return data;
        }

        // Handle Map parameter
        if (entity instanceof Map) {
            Map<?, ?> paramMap = (Map<?, ?>) entity;
            Object et = paramMap.get("et");
            if (et != null) {
                entity = et;
            } else {
                return data;
            }
        }

        try {
            Class<?> clazz = entity.getClass();
            Field[] fields = clazz.getDeclaredFields();
            
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    data.put(field.getName(), value);
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract entity data", e);
        }

        return data;
    }

    /**
     * Extract primary key from entity
     */
    private String extractPrimaryKey(Object entity) {
        if (entity == null) {
            return null;
        }

        // Handle Map parameter
        if (entity instanceof Map) {
            Map<?, ?> paramMap = (Map<?, ?>) entity;
            Object et = paramMap.get("et");
            if (et != null) {
                entity = et;
            }
        }

        try {
            Class<?> clazz = entity.getClass();
            Field idField = findField(clazz, "id");
            if (idField == null) {
                log.debug("No 'id' field found in class: {}", clazz.getName());
                return null;
            }
            idField.setAccessible(true);
            Object id = idField.get(entity);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to extract primary key", e);
            return null;
        }
    }
    
    /**
     * Find field in class hierarchy (including parent classes)
     * 
     * @param clazz the class to search
     * @param fieldName the field name
     * @return the field, or null if not found
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Field not found in current class, try parent class
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Calculate changed fields between before and after data
     */
    private String calculateChangedFields(Map<String, Object> beforeData, Map<String, Object> afterData) {
        StringBuilder changedFields = new StringBuilder();
        
        for (Map.Entry<String, Object> entry : afterData.entrySet()) {
            String fieldName = entry.getKey();
            Object afterValue = entry.getValue();
            Object beforeValue = beforeData.get(fieldName);
            
            if (!isEqual(beforeValue, afterValue)) {
                if (changedFields.length() > 0) {
                    changedFields.append(",");
                }
                changedFields.append(fieldName);
            }
        }
        
        return changedFields.toString();
    }

    /**
     * Check if two values are equal
     */
    private boolean isEqual(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * Extract table name from mapper method ID
     */
    private String extractTableName(String mapperId) {
        // Example: com.basebackend.user.mapper.UserMapper.insert
        // Extract "User" from the mapper name
        if (mapperId == null) {
            return "unknown";
        }
        
        String[] parts = mapperId.split("\\.");
        if (parts.length >= 2) {
            String mapperName = parts[parts.length - 2];
            if (mapperName.endsWith("Mapper")) {
                return mapperName.substring(0, mapperName.length() - 6);
            }
        }
        
        return "unknown";
    }

    /**
     * Check if table is excluded from audit
     */
    private boolean isExcludedTable(String tableName) {
        // Always exclude audit log tables to prevent infinite recursion
        if ("AuditLog".equalsIgnoreCase(tableName) || 
            "AuditLogArchive".equalsIgnoreCase(tableName) ||
            "sys_audit_log".equalsIgnoreCase(tableName) ||
            "sys_audit_log_archive".equalsIgnoreCase(tableName)) {
            return true;
        }
        
        List<String> excludedTables = properties.getAudit().getExcludedTables();
        return excludedTables != null && excludedTables.stream()
                .anyMatch(excluded -> tableName.equalsIgnoreCase(excluded) || 
                                     tableName.toLowerCase().contains(excluded.toLowerCase()));
    }

    /**
     * Get current user ID from security context
     * This is a placeholder - should be implemented based on your security framework
     */
    private Long getCurrentUserId() {
        // TODO: Implement based on your security context
        return null;
    }

    /**
     * Get current user name from security context
     * This is a placeholder - should be implemented based on your security framework
     */
    private String getCurrentUserName() {
        // TODO: Implement based on your security context
        return null;
    }

    /**
     * Get current user IP from request context
     * This is a placeholder - should be implemented based on your web framework
     */
    private String getCurrentUserIp() {
        // TODO: Implement based on your request context
        return null;
    }

    /**
     * Get current tenant ID from tenant context
     * This is a placeholder - should be implemented based on your multi-tenancy implementation
     */
    private String getCurrentTenantId() {
        // TODO: Implement based on your tenant context
        return null;
    }
}
