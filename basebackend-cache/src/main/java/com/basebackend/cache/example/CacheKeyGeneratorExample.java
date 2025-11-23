package com.basebackend.cache.example;

import com.basebackend.cache.service.RedisService;
import com.basebackend.cache.util.CacheKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * CacheKeyGenerator 使用示例
 * 
 * 演示如何在实际业务中使用各种键生成策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheKeyGeneratorExample {
    
    private final CacheKeyGenerator keyGenerator;
    private final RedisService redisService;
    
    /**
     * 示例 1：简单键生成
     * 适用于基本的缓存场景
     */
    public User getUserById(Long userId) {
        // 生成简单键：cache:user:123
        String key = keyGenerator.generateSimpleKey("cache", "user", userId.toString());
        
        Object cached = redisService.get(key);
        if (cached instanceof User) {
            User user = (User) cached;
            log.info("Cache hit for user: {}", userId);
            return user;
        }
        
        // 从数据库查询
        User user = queryUserFromDB(userId);
        
        // 缓存结果
        redisService.set(key, user, 3600, TimeUnit.SECONDS);
        log.info("Cached user: {}", userId);
        
        return user;
    }
    
    /**
     * 示例 2：版本控制键
     * 适用于需要版本管理的缓存
     */
    public ApiResponse getApiData(String apiKey, String version) {
        // 生成版本键：cache:api:v1.0:data
        String key = keyGenerator.generateVersionedKey("cache", "api", apiKey, version);
        
        Object cached = redisService.get(key);
        if (cached instanceof ApiResponse) {
            return (ApiResponse) cached;
        }
        
        // 调用 API
        ApiResponse response = callExternalApi(apiKey);
        redisService.set(key, response, 1800, TimeUnit.SECONDS);
        
        return response;
    }
    
    /**
     * 示例 3：租户键
     * 适用于多租户场景
     */
    public TenantConfig getTenantConfig(String tenantId, String configKey) {
        // 生成租户键：tenant:tenant001:cache:config:key
        String key = keyGenerator.generateTenantKey(tenantId, "cache", "config", configKey);
        
        Object cached = redisService.get(key);
        if (cached instanceof TenantConfig) {
            return (TenantConfig) cached;
        }
        
        // 从数据库查询
        TenantConfig config = queryTenantConfig(tenantId, configKey);
        redisService.set(key, config, 7200, TimeUnit.SECONDS);
        
        return config;
    }
    
    /**
     * 示例 4：分页键
     * 适用于分页查询缓存
     */
    public PageResult<Order> getOrders(int pageNum, int pageSize, OrderQuery query) {
        // 将查询条件转换为 Map
        Map<String, Object> params = new HashMap<>();
        params.put("status", query.getStatus());
        params.put("userId", query.getUserId());
        params.put("startDate", query.getStartDate());
        
        // 生成分页键：cache:orders:page:1_20:startDate=2024-01-01&status=active&userId=123
        String key = keyGenerator.generatePageKey("cache", "orders", pageNum, pageSize, params);
        
        Object cached = redisService.get(key);
        if (cached instanceof PageResult) {
            @SuppressWarnings("unchecked")
            PageResult<Order> result = (PageResult<Order>) cached;
            log.info("Cache hit for orders page: {}/{}", pageNum, pageSize);
            return result;
        }
        
        // 从数据库查询
        PageResult<Order> result = queryOrdersFromDB(pageNum, pageSize, query);
        
        // 缓存 10 分钟
        redisService.set(key, result, 600, TimeUnit.SECONDS);
        
        return result;
    }
    
    /**
     * 示例 5：JSON 键
     * 适用于复杂查询对象
     */
    public Report generateReport(ReportQuery query) {
        // 使用 JSON 键处理复杂对象：cache:report:{json_hash}
        String key = keyGenerator.generateJsonKey("cache", "report", query);
        
        Object cached = redisService.get(key);
        if (cached instanceof Report) {
            log.info("Cache hit for report");
            return (Report) cached;
        }
        
        // 生成报表
        Report report = generateReportFromDB(query);
        
        // 缓存 1 小时
        redisService.set(key, report, 3600, TimeUnit.SECONDS);
        
        return report;
    }
    
    /**
     * 示例 6：批量操作
     * 适用于批量查询场景
     */
    public Map<Long, User> getUsersByIds(List<Long> userIds) {
        // 生成批量键
        List<String> keys = keyGenerator.generateBatchKeys("cache", "user", userIds);
        
        // 批量获取
        Set<String> keySet = new HashSet<>(keys);
        Map<String, Object> cachedObjects = redisService.multiGet(keySet);
        Map<String, User> cachedUsers = new HashMap<>();
        for (Map.Entry<String, Object> entry : cachedObjects.entrySet()) {
            if (entry.getValue() instanceof User) {
                cachedUsers.put(entry.getKey(), (User) entry.getValue());
            }
        }
        
        // 找出未命中的 ID
        Set<Long> missedIds = new HashSet<>();
        for (int i = 0; i < userIds.size(); i++) {
            if (!cachedUsers.containsKey(keys.get(i))) {
                missedIds.add(userIds.get(i));
            }
        }
        
        // 查询未命中的数据
        if (!missedIds.isEmpty()) {
            List<User> missedUsers = queryUsersFromDB(new ArrayList<>(missedIds));
            
            // 批量缓存
            Map<String, Object> toCache = new HashMap<>();
            for (User user : missedUsers) {
                String key = keyGenerator.generateSimpleKey("cache", "user", user.getId().toString());
                toCache.put(key, user);
            }
            redisService.multiSet(toCache, java.time.Duration.ofSeconds(3600));
            
            // 合并结果
            for (Map.Entry<String, Object> entry : toCache.entrySet()) {
                if (entry.getValue() instanceof User) {
                    cachedUsers.put(entry.getKey(), (User) entry.getValue());
                }
            }
        }
        
        // 转换为 ID -> User 的 Map
        Map<Long, User> result = new HashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            User user = cachedUsers.get(keys.get(i));
            if (user != null) {
                result.put(userIds.get(i), user);
            }
        }
        
        return result;
    }
    
    /**
     * 示例 7：命名空间键
     * 适用于多环境场景
     */
    public Config getConfig(String environment, String configKey) {
        // 生成命名空间键：prod:cache:config:key
        String key = keyGenerator.generateNamespacedKey(environment, "cache", "config", configKey);
        
        Object cached = redisService.get(key);
        if (cached instanceof Config) {
            return (Config) cached;
        }
        
        // 从配置中心查询
        Config config = queryConfigFromCenter(environment, configKey);
        redisService.set(key, config, 3600, TimeUnit.SECONDS);
        
        return config;
    }
    
    /**
     * 示例 8：清除租户缓存
     * 演示如何使用模式键清除缓存
     */
    public void clearTenantCache(String tenantId) {
        // 生成租户模式键：tenant:tenant001:*
        String pattern = "tenant:" + tenantId + ":*";
        
        long deletedCount = redisService.deleteByPattern(pattern);
        log.info("Cleared {} cache entries for tenant: {}", deletedCount, tenantId);
    }
    
    /**
     * 示例 9：清除版本缓存
     * 演示如何清除特定版本的缓存
     */
    public void clearVersionCache(String version) {
        // 生成版本模式键：cache:api:v1.0:*
        String pattern = keyGenerator.generateVersionedKey("cache", "api", "*", version);
        
        long deletedCount = redisService.deleteByPattern(pattern);
        log.info("Cleared {} cache entries for version: {}", deletedCount, version);
    }
    
    /**
     * 示例 10：时间戳键
     * 适用于时间敏感的缓存
     */
    public DailyReport getDailyReport(String reportType, long timestamp) {
        // 生成时间戳键：cache:report:daily:1700000000000
        String key = keyGenerator.generateTimestampKey("cache", "report", reportType, timestamp);
        
        Object cached = redisService.get(key);
        if (cached instanceof DailyReport) {
            return (DailyReport) cached;
        }
        
        // 生成报表
        DailyReport report = generateDailyReport(reportType, timestamp);
        
        // 缓存 24 小时
        redisService.set(key, report, 86400, TimeUnit.SECONDS);
        
        return report;
    }
    
    // ========== 模拟数据库查询方法 ==========
    
    private User queryUserFromDB(Long userId) {
        // 模拟数据库查询
        return new User(userId, "User" + userId);
    }
    
    private List<User> queryUsersFromDB(List<Long> userIds) {
        // 模拟批量查询
        List<User> users = new ArrayList<>();
        for (Long id : userIds) {
            users.add(new User(id, "User" + id));
        }
        return users;
    }
    
    private TenantConfig queryTenantConfig(String tenantId, String configKey) {
        return new TenantConfig(tenantId, configKey, "value");
    }
    
    private PageResult<Order> queryOrdersFromDB(int pageNum, int pageSize, OrderQuery query) {
        return new PageResult<>();
    }
    
    private Report generateReportFromDB(ReportQuery query) {
        return new Report();
    }
    
    private ApiResponse callExternalApi(String apiKey) {
        return new ApiResponse();
    }
    
    private Config queryConfigFromCenter(String environment, String configKey) {
        return new Config();
    }
    
    private DailyReport generateDailyReport(String reportType, long timestamp) {
        return new DailyReport();
    }
    
    // ========== 示例数据类 ==========
    
    public static class User {
        private Long id;
        private String name;
        
        public User(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public Long getId() {
            return id;
        }
    }
    
    public static class TenantConfig {
        private String tenantId;
        private String key;
        private String value;
        
        public TenantConfig(String tenantId, String key, String value) {
            this.tenantId = tenantId;
            this.key = key;
            this.value = value;
        }
    }
    
    public static class OrderQuery {
        private String status;
        private Long userId;
        private String startDate;
        
        public String getStatus() { return status; }
        public Long getUserId() { return userId; }
        public String getStartDate() { return startDate; }
    }
    
    public static class PageResult<T> {
        private List<T> data;
        private int total;
    }
    
    public static class Report {}
    public static class ReportQuery {}
    public static class ApiResponse {}
    public static class Config {}
    public static class DailyReport {}
    public static class Order {}
}
