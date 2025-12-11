# Audit Interceptor Infinite Recursion Fix

## Problem Description

### Symptom
The `AuditInterceptor` was causing an infinite recursion loop when saving audit logs, resulting in:
- `Data too long for column 'after_data'` errors
- Stack overflow or excessive data accumulation
- Application performance degradation

### Root Cause
The audit interceptor was intercepting **ALL** database operations, including its own INSERT operations to the `sys_audit_log` table. This created an infinite loop:

```
1. User performs operation (e.g., login)
2. AuditInterceptor intercepts and saves audit log
3. Saving audit log triggers INSERT to sys_audit_log
4. AuditInterceptor intercepts the audit log INSERT
5. Tries to save another audit log for the audit log operation
6. Go to step 3 (infinite loop)
```

## Solution Applied

### Two-Layer Protection

#### 1. Mapper-Level Check (Primary Defense)
Added early detection to skip `AuditLogMapper` operations entirely:

```java
// Skip audit log mapper operations to prevent infinite recursion
if (mapperId != null && mapperId.contains("AuditLogMapper")) {
    return invocation.proceed();
}
```

This catches the operation **before** any processing, providing the fastest exit path.

#### 2. Table-Level Check (Secondary Defense)
Enhanced `isExcludedTable()` to always exclude audit-related tables:

```java
// Always exclude audit log tables to prevent infinite recursion
if ("AuditLog".equalsIgnoreCase(tableName) || 
    "AuditLogArchive".equalsIgnoreCase(tableName) ||
    "sys_audit_log".equalsIgnoreCase(tableName) ||
    "sys_audit_log_archive".equalsIgnoreCase(tableName)) {
    return true;
}
```

This provides defense-in-depth in case the mapper name check fails.

## Why This Approach

### Advantages
1. **Performance**: Mapper check happens early, minimal overhead
2. **Reliability**: Two layers of protection ensure no recursion
3. **Maintainability**: Clear and explicit exclusion logic
4. **Flexibility**: Still respects user-configured excluded tables

### Alternative Approaches Considered
- ❌ **ThreadLocal flag**: Complex, error-prone with async operations
- ❌ **Configuration only**: Easy to forget, not fail-safe
- ✅ **Hard-coded exclusion**: Simple, reliable, self-documenting

## Testing

After applying this fix:

1. **Restart the application** (required for changes to take effect)
2. Perform operations that trigger audit logging (e.g., user login, data updates)
3. Verify:
   - ✅ Audit logs are created successfully
   - ✅ No infinite recursion errors
   - ✅ `after_data` field contains reasonable data size
   - ✅ No performance degradation

## Related Files

- `basebackend-database/src/main/java/com/basebackend/database/audit/interceptor/AuditInterceptor.java` - Fixed interceptor
- `basebackend-database/src/main/java/com/basebackend/database/audit/entity/AuditLog.java` - Audit log entity
- `basebackend-database/src/main/java/com/basebackend/database/audit/mapper/AuditLogMapper.java` - Audit log mapper

## Configuration

No configuration changes required. The fix is automatic and always active.

If you need to exclude additional tables from auditing, use the configuration:

```yaml
database:
  enhanced:
    audit:
      enabled: true
      excluded-tables:
        - sys_config
        - sys_dict
        # Add more tables as needed
```

## Impact

- ✅ Fixes infinite recursion bug
- ✅ Prevents data truncation errors
- ✅ Improves application stability
- ✅ No breaking changes to existing functionality
- ✅ Minimal performance overhead
