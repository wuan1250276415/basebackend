package com.basebackend.cache.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 缓存键生成器
 * 负责生成缓存键，支持自定义键和默认键生成策略
 * 
 * 功能特性：
 * 1. 支持多种键生成策略（默认、简单、哈希、JSON）
 * 2. 支持复杂对象序列化为键
 * 3. 支持键长度限制和自动哈希
 * 4. 支持模式匹配键生成
 * 5. 支持键命名空间管理
 * 6. 支持键版本控制
 */
@Slf4j
@Component
public class CacheKeyGenerator {
    
    /**
     * 默认键最大长度
     */
    private static final int DEFAULT_MAX_KEY_LENGTH = 200;
    
    /**
     * 键分隔符
     */
    private static final String KEY_SEPARATOR = ":";
    
    /**
     * 通配符
     */
    private static final String WILDCARD = "*";
    
    /**
     * JSON 序列化器
     */
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 生成缓存键
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param customKey 自定义键（可能包含 SpEL 表达式解析后的值）
     * @param target 目标对象
     * @param method 方法
     * @param args 方法参数
     * @return 完整的缓存键
     */
    public String generateKey(String prefix, String cacheName, String customKey, 
                             Object target, Method method, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 1. 添加前缀
        if (StringUtils.hasText(prefix)) {
            keyBuilder.append(prefix).append(":");
        }
        
        // 2. 添加缓存名称
        if (StringUtils.hasText(cacheName)) {
            keyBuilder.append(cacheName);
        } else {
            // 如果没有指定缓存名称，使用类名
            keyBuilder.append(target.getClass().getSimpleName());
        }
        keyBuilder.append(":");
        
        // 3. 添加键部分
        if (StringUtils.hasText(customKey)) {
            // 使用自定义键（已经过 SpEL 解析）
            keyBuilder.append(customKey);
        } else {
            // 使用默认键生成策略：方法名 + 参数
            keyBuilder.append(generateDefaultKey(method, args));
        }
        
        String finalKey = keyBuilder.toString();
        log.debug("Generated cache key: {}", finalKey);
        
        return finalKey;
    }
    
    /**
     * 生成默认缓存键
     * 格式：methodName:arg1_arg2_arg3
     * 
     * @param method 方法
     * @param args 方法参数
     * @return 默认键
     */
    private String generateDefaultKey(Method method, Object[] args) {
        StringBuilder keyBuilder = new StringBuilder();
        
        // 添加方法名
        keyBuilder.append(method.getName());
        
        // 添加参数
        if (args != null && args.length > 0) {
            keyBuilder.append(":");
            String argsKey = Arrays.stream(args)
                    .map(this::convertArgToString)
                    .collect(Collectors.joining("_"));
            keyBuilder.append(argsKey);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 将参数转换为字符串
     * 
     * @param arg 参数
     * @return 字符串表示
     */
    private String convertArgToString(Object arg) {
        if (arg == null) {
            return "null";
        }
        
        // 对于基本类型和字符串，直接转换
        if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
            return arg.toString();
        }
        
        // 对于数组，使用 Arrays.toString
        if (arg.getClass().isArray()) {
            if (arg instanceof Object[]) {
                return Arrays.toString((Object[]) arg);
            } else if (arg instanceof int[]) {
                return Arrays.toString((int[]) arg);
            } else if (arg instanceof long[]) {
                return Arrays.toString((long[]) arg);
            } else if (arg instanceof double[]) {
                return Arrays.toString((double[]) arg);
            } else if (arg instanceof boolean[]) {
                return Arrays.toString((boolean[]) arg);
            }
            // 其他基本类型数组...
            return arg.toString();
        }
        
        // 对于复杂对象，使用 hashCode
        // 这样可以保证相同对象生成相同的键
        return String.valueOf(arg.hashCode());
    }
    
    /**
     * 生成模式匹配键
     * 用于批量删除操作
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @return 模式匹配键（例如：cache:UserService:*）
     */
    public String generatePatternKey(String prefix, String cacheName) {
        StringBuilder keyBuilder = new StringBuilder();
        
        if (StringUtils.hasText(prefix)) {
            keyBuilder.append(prefix).append(":");
        }
        
        if (StringUtils.hasText(cacheName)) {
            keyBuilder.append(cacheName);
        } else {
            keyBuilder.append("*");
        }
        
        keyBuilder.append(":*");
        
        return keyBuilder.toString();
    }
    
    /**
     * 生成简单键
     * 不包含方法和参数信息，仅使用前缀和缓存名称
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @return 简单键
     */
    public String generateSimpleKey(String prefix, String cacheName, String key) {
        StringBuilder keyBuilder = new StringBuilder();
        
        if (StringUtils.hasText(prefix)) {
            keyBuilder.append(prefix).append(":");
        }
        
        if (StringUtils.hasText(cacheName)) {
            keyBuilder.append(cacheName).append(":");
        }
        
        if (StringUtils.hasText(key)) {
            keyBuilder.append(key);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 生成哈希键
     * 对于过长的键，使用 MD5 哈希
     * 
     * @param key 原始键
     * @return 哈希后的键
     */
    public String generateHashKey(String key) {
        return generateHashKey(key, DEFAULT_MAX_KEY_LENGTH);
    }
    
    /**
     * 生成哈希键（指定最大长度）
     * 
     * @param key 原始键
     * @param maxLength 最大长度
     * @return 哈希后的键
     */
    public String generateHashKey(String key, int maxLength) {
        if (key.length() <= maxLength) {
            return key;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHex(digest);
            
            // 保留原始键的前缀部分，便于识别
            int prefixLength = Math.min(50, key.length());
            String prefix = key.substring(0, prefixLength);
            return prefix + KEY_SEPARATOR + hash;
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate hash key", e);
            return key;
        }
    }
    
    /**
     * 生成 SHA-256 哈希键
     * 比 MD5 更安全，适用于安全性要求较高的场景
     * 
     * @param key 原始键
     * @return SHA-256 哈希键
     */
    public String generateSHA256Key(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate SHA-256 key", e);
            return generateHashKey(key);
        }
    }
    
    /**
     * 字节数组转十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * 生成基于 JSON 的键
     * 将复杂对象序列化为 JSON 字符串作为键的一部分
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param obj 对象
     * @return JSON 键
     */
    public String generateJsonKey(String prefix, String cacheName, Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            String key = buildKey(prefix, cacheName, json);
            
            // 如果键过长，使用哈希
            return generateHashKey(key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON for cache key", e);
            // 降级为使用 hashCode
            return buildKey(prefix, cacheName, String.valueOf(obj.hashCode()));
        }
    }
    
    /**
     * 生成带版本的键
     * 用于缓存版本控制，方便批量失效旧版本缓存
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @param version 版本号
     * @return 带版本的键
     */
    public String generateVersionedKey(String prefix, String cacheName, String key, String version) {
        return buildKey(prefix, cacheName, "v" + version, key);
    }
    
    /**
     * 生成带命名空间的键
     * 用于多租户或多环境场景
     * 
     * @param namespace 命名空间
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @return 带命名空间的键
     */
    public String generateNamespacedKey(String namespace, String prefix, String cacheName, String key) {
        return buildKey(namespace, prefix, cacheName, key);
    }
    
    /**
     * 生成带租户的键
     * 用于多租户场景
     * 
     * @param tenantId 租户ID
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @return 带租户的键
     */
    public String generateTenantKey(String tenantId, String prefix, String cacheName, String key) {
        return buildKey("tenant", tenantId, prefix, cacheName, key);
    }
    
    /**
     * 生成集合键
     * 用于缓存集合类型的数据
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param collectionName 集合名称
     * @return 集合键
     */
    public String generateCollectionKey(String prefix, String cacheName, String collectionName) {
        return buildKey(prefix, cacheName, "collection", collectionName);
    }
    
    /**
     * 生成分页键
     * 用于缓存分页数据
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 分页键
     */
    public String generatePageKey(String prefix, String cacheName, int pageNum, int pageSize) {
        return buildKey(prefix, cacheName, "page", pageNum + "_" + pageSize);
    }
    
    /**
     * 生成分页键（带查询条件）
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param queryParams 查询参数
     * @return 分页键
     */
    public String generatePageKey(String prefix, String cacheName, int pageNum, int pageSize, 
                                  Map<String, Object> queryParams) {
        String paramsKey = generateMapKey(queryParams);
        return buildKey(prefix, cacheName, "page", pageNum + "_" + pageSize, paramsKey);
    }
    
    /**
     * 生成列表键
     * 用于缓存列表数据
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param listName 列表名称
     * @return 列表键
     */
    public String generateListKey(String prefix, String cacheName, String listName) {
        return buildKey(prefix, cacheName, "list", listName);
    }
    
    /**
     * 生成 Map 键
     * 将 Map 参数转换为键字符串
     * 
     * @param params 参数 Map
     * @return Map 键
     */
    public String generateMapKey(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "empty";
        }
        
        // 按键排序，确保相同参数生成相同的键
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + convertArgToString(entry.getValue()))
                .collect(Collectors.joining("&"));
    }
    
    /**
     * 生成带时间戳的键
     * 用于需要时间敏感的缓存
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @param timestamp 时间戳
     * @return 带时间戳的键
     */
    public String generateTimestampKey(String prefix, String cacheName, String key, long timestamp) {
        return buildKey(prefix, cacheName, key, String.valueOf(timestamp));
    }
    
    /**
     * 生成带过期时间的键
     * 将过期时间编码到键中，用于特殊场景
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param key 键值
     * @param ttlSeconds TTL（秒）
     * @return 带过期时间的键
     */
    public String generateTTLKey(String prefix, String cacheName, String key, long ttlSeconds) {
        return buildKey(prefix, cacheName, "ttl" + ttlSeconds, key);
    }
    
    /**
     * 解析键的各个部分
     * 
     * @param key 完整的键
     * @return 键的各个部分
     */
    public List<String> parseKey(String key) {
        if (!StringUtils.hasText(key)) {
            return Collections.emptyList();
        }
        return Arrays.asList(key.split(KEY_SEPARATOR));
    }
    
    /**
     * 提取键的前缀
     * 
     * @param key 完整的键
     * @return 前缀
     */
    public String extractPrefix(String key) {
        List<String> parts = parseKey(key);
        return parts.isEmpty() ? "" : parts.get(0);
    }
    
    /**
     * 提取键的缓存名称
     * 
     * @param key 完整的键
     * @return 缓存名称
     */
    public String extractCacheName(String key) {
        List<String> parts = parseKey(key);
        return parts.size() > 1 ? parts.get(1) : "";
    }
    
    /**
     * 构建键
     * 将多个部分组合成完整的键
     * 
     * @param parts 键的各个部分
     * @return 完整的键
     */
    private String buildKey(String... parts) {
        return Arrays.stream(parts)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(KEY_SEPARATOR));
    }
    
    /**
     * 验证键是否有效
     * 
     * @param key 键
     * @return 是否有效
     */
    public boolean isValidKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        
        // 检查键长度
        if (key.length() > 1024) {
            log.warn("Cache key is too long: {} characters", key.length());
            return false;
        }
        
        // 检查是否包含非法字符
        if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
            log.warn("Cache key contains illegal characters");
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理键
     * 移除非法字符
     * 
     * @param key 原始键
     * @return 清理后的键
     */
    public String sanitizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        
        // 移除空格、换行等字符
        String sanitized = key.replaceAll("\\s+", "_");
        
        // 如果键过长，进行哈希
        if (sanitized.length() > DEFAULT_MAX_KEY_LENGTH) {
            sanitized = generateHashKey(sanitized);
        }
        
        return sanitized;
    }
    
    /**
     * 生成批量操作的键列表
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param ids ID列表
     * @return 键列表
     */
    public List<String> generateBatchKeys(String prefix, String cacheName, Collection<?> ids) {
        return ids.stream()
                .map(id -> generateSimpleKey(prefix, cacheName, String.valueOf(id)))
                .collect(Collectors.toList());
    }
    
    /**
     * 生成范围查询的模式键
     * 
     * @param prefix 键前缀
     * @param cacheName 缓存名称
     * @param rangeStart 范围起始
     * @param rangeEnd 范围结束
     * @return 模式键
     */
    public String generateRangePatternKey(String prefix, String cacheName, 
                                         String rangeStart, String rangeEnd) {
        return buildKey(prefix, cacheName, "range", rangeStart + "_" + rangeEnd, WILDCARD);
    }
}
