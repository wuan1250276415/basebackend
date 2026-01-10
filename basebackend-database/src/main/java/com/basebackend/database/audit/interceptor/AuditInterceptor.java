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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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

    // Performance monitoring
    private static final AtomicLong TOTAL_AUDIT_OPERATIONS = new AtomicLong(0);
    private static final AtomicLong FAILED_AUDIT_OPERATIONS = new AtomicLong(0);
    private static final ConcurrentHashMap<String, AtomicLong> OPERATION_COUNTS = new ConcurrentHashMap<>();

    // Operation ID generator for tracking
    private static final AtomicLong OPERATION_ID_GENERATOR = new AtomicLong(0);

    // Field cache for reflection (enhanced from P1)
    private static final ConcurrentHashMap<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

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

        // Generate unique operation ID for tracking
        long operationId = OPERATION_ID_GENERATOR.incrementAndGet();

        // Skip audit log mapper operations to prevent infinite recursion
        if (mapperId != null && mapperId.contains("AuditLogMapper")) {
            return invocation.proceed();
        }

        String tableName = extractTableName(mapperId);

        // Skip excluded tables
        if (isExcludedTable(tableName)) {
            return invocation.proceed();
        }

        long startTime = System.currentTimeMillis();
        TOTAL_AUDIT_OPERATIONS.incrementAndGet();

        // Track operation count
        OPERATION_COUNTS.computeIfAbsent(sqlCommandType.name(), k -> new AtomicLong(0)).incrementAndGet();

        log.debug("Audit operation started: operationId={}, table={}, operation={}, mapperId={}",
            operationId, tableName, sqlCommandType.name(), mapperId);

        try {
            // Capture before data for UPDATE operations
            if (sqlCommandType == SqlCommandType.UPDATE) {
                captureBeforeData(parameter, operationId);
            }

            // Execute the actual update
            Object result = invocation.proceed();

            // Record audit log after successful execution
            int affectedRows = (Integer) result;
            recordAuditLog(sqlCommandType, tableName, parameter, affectedRows, operationId);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Audit operation completed successfully: operationId={}, table={}, rows={}, duration={}ms",
                operationId, tableName, affectedRows, duration);

            return result;
        } catch (Exception e) {
            FAILED_AUDIT_OPERATIONS.incrementAndGet();
            long duration = System.currentTimeMillis() - startTime;

            // Enhanced error logging with operation context
            log.error("Audit operation failed: operationId={}, table={}, operation={}, mapperId={}, duration={}ms, error={}",
                operationId, tableName, sqlCommandType.name(), mapperId, duration, e.getMessage(), e);

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
    private void recordAuditLog(SqlCommandType sqlCommandType, String tableName, Object parameter, int result, long operationId) {
        if (result == 0) {
            log.debug("Audit skipped: operationId={}, table={}, no rows affected", operationId, tableName);
            return; // No rows affected, skip audit
        }

        try {
            AuditLog auditLog = buildAuditLog(sqlCommandType, tableName, parameter, result, operationId);

            if (auditLog != null) {
                if (properties.getAudit().isAsync()) {
                    auditLogService.logAsync(auditLog);
                    log.trace("Audit log queued asynchronously: operationId={}", operationId);
                } else {
                    auditLogService.log(auditLog);
                    log.trace("Audit log recorded synchronously: operationId={}", operationId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to record audit log: operationId={}, table={}, error={}", operationId, tableName, e.getMessage(), e);
        }
    }

    /**
     * Capture before data for UPDATE operations
     */
    private void captureBeforeData(Object parameter, long operationId) {
        if (parameter == null) {
            return;
        }

        try {
            Map<String, Object> beforeData = new HashMap<>();

            if (parameter instanceof Map<?, ?> paramMap) {
                // Try to get entity from 'et' parameter
                Object entity = null;
                try {
                    entity = paramMap.get("et");
                } catch (Exception e) {
                    log.trace("No 'et' parameter when capturing before data: operationId={}", operationId);
                }
                
                if (entity != null) {
                    beforeData = extractEntityData(entity, operationId);
                } else {
                    // Try 'param1' as fallback
                    Object param1 = paramMap.get("param1");
                    if (param1 != null && !isPrimitiveOrWrapper(param1.getClass())) {
                        beforeData = extractEntityData(param1, operationId);
                    }
                }
            } else {
                beforeData = extractEntityData(parameter, operationId);
            }

            BEFORE_DATA.set(beforeData);
            log.trace("Before data captured: operationId={}, fieldCount={}", operationId, beforeData.size());
        } catch (Exception e) {
            log.error("Failed to capture before data: operationId={}, error={}", operationId, e.getMessage(), e);
        }
    }

    /**
     * Build audit log
     */
    private AuditLog buildAuditLog(SqlCommandType sqlCommandType, String tableName, Object parameter, int result, long operationId) {
        if (result == 0) {
            return null; // No rows affected, skip audit
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setOperationType(sqlCommandType.name());
        auditLog.setTableName(tableName);
        auditLog.setOperateTime(new Date());

        try {
            Map<String, Object> afterData = extractEntityData(parameter, operationId);

            // Extract primary key
            String primaryKey = extractPrimaryKey(parameter, operationId);
            auditLog.setPrimaryKey(primaryKey);

            // Set after data
            auditLog.setAfterData(objectMapper.writeValueAsString(afterData));

            // For UPDATE operations, set before data and changed fields
            if (sqlCommandType == SqlCommandType.UPDATE) {
                Map<String, Object> beforeData = BEFORE_DATA.get();
                if (beforeData != null && !beforeData.isEmpty()) {
                    auditLog.setBeforeData(objectMapper.writeValueAsString(beforeData));
                    auditLog.setChangedFields(calculateChangedFields(beforeData, afterData, operationId));
                }
            }

            // For DELETE operations, store the deleted data in beforeData
            if (sqlCommandType == SqlCommandType.DELETE) {
                auditLog.setBeforeData(objectMapper.writeValueAsString(afterData));
            }

            // Set operator information with enhanced Spring Security integration
            auditLog.setOperatorId(getCurrentUserId());
            auditLog.setOperatorName(getCurrentUserName());
            auditLog.setOperatorIp(getCurrentUserIp());
            auditLog.setTenantId(getCurrentTenantId());

            log.trace("Audit log built successfully: operationId={}, table={}, primaryKey={}",
                operationId, tableName, primaryKey);

        } catch (Exception e) {
            log.error("Failed to build audit log: operationId={}, table={}, error={}", operationId, tableName, e.getMessage(), e);
            return null;
        }

        return auditLog;
    }

    /**
     * Extract entity data using reflection with caching
     */
    private Map<String, Object> extractEntityData(Object entity, long operationId) {
        Map<String, Object> data = new HashMap<>();

        if (entity == null) {
            return data;
        }

        // Handle Map parameter
        if (entity instanceof Map<?, ?> paramMap) {
            // Try to get 'et' parameter (entity wrapper from MyBatis)
            Object et = null;
            try {
                et = paramMap.get("et");
            } catch (Exception e) {
                log.trace("No 'et' parameter found in map: operationId={}", operationId);
            }
            
            if (et != null) {
                entity = et;
            } else {
                // Try to get 'param1' (first parameter from MyBatis)
                Object param1 = paramMap.get("param1");
                if (param1 != null && !isPrimitiveOrWrapper(param1.getClass())) {
                    entity = param1;
                } else {
                    // If no entity object found, return the map as-is (for primitive parameters)
                    log.debug("No entity object found in parameters, using map: operationId={}, params={}", 
                        operationId, paramMap.keySet());
                    return new HashMap<>((Map<String, Object>) paramMap);
                }
            }
        }

        try {
            Class<?> clazz = entity.getClass();
            Field[] fields = getCachedFields(clazz);

            for (Field field : fields) {
                // Skip static and synthetic fields
                if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                Object value = field.get(entity);
                if (value != null) {
                    data.put(field.getName(), value);
                }
            }

            log.trace("Entity data extracted: operationId={}, class={}, fieldCount={}",
                operationId, clazz.getSimpleName(), data.size());
        } catch (Exception e) {
            log.error("Failed to extract entity data: operationId={}, class={}, error={}",
                operationId, entity.getClass().getName(), e.getMessage(), e);
        }

        return data;
    }

    /**
     * Get cached fields for a class
     */
    private Field[] getCachedFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, k -> {
            List<Field> allFields = new ArrayList<>();
            Class<?> current = clazz;

            // Get all fields including inherited ones
            while (current != null && current != Object.class) {
                Field[] declaredFields = current.getDeclaredFields();
                for (Field field : declaredFields) {
                    // Skip synthetic and static fields
                    if (!field.isSynthetic() && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        allFields.add(field);
                        field.setAccessible(true); // Pre-set accessible for performance
                    }
                }
                current = current.getSuperclass();
            }

            return allFields.toArray(new Field[0]);
        });
    }

    /**
     * Extract primary key from entity
     */
    private String extractPrimaryKey(Object entity, long operationId) {
        if (entity == null) {
            return null;
        }

        // Handle Map parameter
        if (entity instanceof Map) {
            Map<?, ?> paramMap = (Map<?, ?>) entity;
            // Try to get 'et' parameter
            Object et = null;
            try {
                et = paramMap.get("et");
            } catch (Exception e) {
                log.trace("No 'et' parameter found when extracting primary key: operationId={}", operationId);
            }
            
            if (et != null) {
                entity = et;
            } else {
                // Try to get 'param1' as fallback
                Object param1 = paramMap.get("param1");
                if (param1 != null && !isPrimitiveOrWrapper(param1.getClass())) {
                    entity = param1;
                } else {
                    // No entity object found, return null
                    log.debug("Cannot extract primary key from primitive parameters: operationId={}", operationId);
                    return null;
                }
            }
        }

        try {
            Class<?> clazz = entity.getClass();
            Field idField = findField(clazz, "id");
            if (idField == null) {
                log.debug("No 'id' field found: operationId={}, class={}", operationId, clazz.getName());
                return null;
            }
            idField.setAccessible(true);
            Object id = idField.get(entity);
            String primaryKey = id != null ? id.toString() : null;
            log.trace("Primary key extracted: operationId={}, primaryKey={}", operationId, primaryKey);
            return primaryKey;
        } catch (Exception e) {
            log.debug("Failed to extract primary key: operationId={}, error={}", operationId, e.getMessage());
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
    private String calculateChangedFields(Map<String, Object> beforeData, Map<String, Object> afterData, long operationId) {
        if (beforeData == null || afterData == null) {
            return "";
        }

        StringBuilder changedFields = new StringBuilder();
        int changeCount = 0;

        for (Map.Entry<String, Object> entry : afterData.entrySet()) {
            String fieldName = entry.getKey();
            Object afterValue = entry.getValue();
            Object beforeValue = beforeData.get(fieldName);

            if (!isEqual(beforeValue, afterValue)) {
                if (changedFields.length() > 0) {
                    changedFields.append(",");
                }
                changedFields.append(fieldName);
                changeCount++;
            }
        }

        log.trace("Changed fields calculated: operationId={}, changedFieldCount={}, fields={}",
            operationId, changeCount, changedFields.toString());

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
     * Get current user ID from Spring Security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    return (Long) principal;
                } else if (principal instanceof String) {
                    String idStr = (String) principal;
                    try {
                        return Long.parseLong(idStr);
                    } catch (NumberFormatException e) {
                        log.debug("Failed to parse user ID from String: {}", idStr);
                    }
                } else if (principal instanceof Integer) {
                    return ((Integer) principal).longValue();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID", e);
        }
        return null;
    }

    /**
     * Get current user name from Spring Security context
     */
    private String getCurrentUserName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.User) {
                    return ((org.springframework.security.core.userdetails.User) principal).getUsername();
                } else if (principal instanceof String) {
                    return (String) principal;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user name", e);
        }
        return null;
    }

    /**
     * Get current user IP from request context (supports proxy headers)
     */
    private String getCurrentUserIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Check X-Forwarded-For header first (for load balancers/proxies)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }

                // Check X-Real-IP header (for Nginx)
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }

                // Fallback to remote address
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Failed to get current user IP", e);
        }
        return null;
    }

    /**
     * Get current tenant ID from tenant context
     * Note: This is a placeholder - implement based on your multi-tenancy strategy
     */
    private String getCurrentTenantId() {
        // TODO: Implement based on your multi-tenancy approach
        // Examples:
        // - ThreadLocal storage
        // - Security context
        // - Database lookup
        return null;
    }

    /**
     * Get audit performance statistics
     * Useful for monitoring and debugging
     */
    public Map<String, Object> getPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAuditOperations", TOTAL_AUDIT_OPERATIONS.get());
        stats.put("failedAuditOperations", FAILED_AUDIT_OPERATIONS.get());
        stats.put("successRate", TOTAL_AUDIT_OPERATIONS.get() > 0 ?
            ((double) (TOTAL_AUDIT_OPERATIONS.get() - FAILED_AUDIT_OPERATIONS.get()) / TOTAL_AUDIT_OPERATIONS.get()) * 100 : 0);

        Map<String, Long> operationStats = new HashMap<>();
        OPERATION_COUNTS.forEach((op, count) -> operationStats.put(op, count.get()));
        stats.put("operationCounts", operationStats);
        stats.put("cachedFieldTypes", FIELD_CACHE.size());

        return stats;
    }

    /**
     * Reset performance counters (useful for testing)
     */
    public void resetPerformanceCounters() {
        TOTAL_AUDIT_OPERATIONS.set(0);
        FAILED_AUDIT_OPERATIONS.set(0);
        OPERATION_COUNTS.clear();
        log.info("Audit interceptor performance counters reset");
    }

    /**
     * Check if a class is a primitive type or wrapper
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Character.class ||
               clazz == Short.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class ||
               clazz == String.class ||
               clazz == java.util.Date.class ||
               clazz == java.time.LocalDate.class ||
               clazz == java.time.LocalDateTime.class ||
               clazz == java.time.LocalTime.class ||
               java.util.Collection.class.isAssignableFrom(clazz) ||
               java.util.Map.class.isAssignableFrom(clazz);
    }
}
