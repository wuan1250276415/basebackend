# Jackson JSR310 LocalDateTime Serialization Fix

## Problem
The `AuditInterceptor` was throwing the following exception when trying to serialize audit log data containing `LocalDateTime` fields:

```
com.fasterxml.jackson.databind.exc.InvalidDefinitionException: 
Java 8 date/time type `java.time.LocalDateTime` not supported by default: 
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

## Root Cause
Jackson's `ObjectMapper` doesn't support Java 8 date/time types (LocalDateTime, LocalDate, etc.) by default. The JSR310 module needs to be:
1. Added as a dependency
2. Registered with the ObjectMapper

## Solution Applied

### 1. Added JSR310 Dependency
Added `jackson-datatype-jsr310` to `basebackend-database/pom.xml`:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

### 2. Configured ObjectMapper Bean
Added ObjectMapper configuration in `MyBatisPlusConfig.java`:

```java
@Bean
@ConditionalOnMissingBean(ObjectMapper.class)
public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    // Register JavaTimeModule for Java 8 date/time support
    mapper.registerModule(new JavaTimeModule());
    // Use ISO-8601 format instead of timestamps
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
}
```

## Key Features
- **@ConditionalOnMissingBean**: Only creates this ObjectMapper if no other exists (respects Spring Boot's auto-configuration)
- **JavaTimeModule**: Enables serialization/deserialization of Java 8 date/time types
- **ISO-8601 Format**: Dates are serialized as readable strings (e.g., "2025-11-23T10:30:00") instead of timestamps

## Impact
- ✅ Fixes audit log serialization errors
- ✅ Supports LocalDateTime, LocalDate, LocalTime, Instant, etc.
- ✅ Produces human-readable date formats in JSON
- ✅ Compatible with existing Spring Boot ObjectMapper configurations

## Testing
After applying this fix:
1. Rebuild the project: `mvn clean install`
2. Test any database operations that trigger audit logging
3. Verify audit logs are properly saved with date/time fields

## Related Files
- `basebackend-database/pom.xml` - Added dependency
- `basebackend-database/src/main/java/com/basebackend/database/config/MyBatisPlusConfig.java` - ObjectMapper configuration
- `basebackend-database/src/main/java/com/basebackend/database/audit/interceptor/AuditInterceptor.java` - Uses ObjectMapper for audit log serialization
