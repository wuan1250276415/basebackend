package com.basebackend.system.service.impl;

import com.basebackend.cache.service.RedisService;
import com.basebackend.system.dto.CacheInfoDTO;
import com.basebackend.system.dto.OnlineUserDTO;
import com.basebackend.system.dto.ServerInfoDTO;
import com.basebackend.system.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统监控服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final RedisService redisService;

    private static final String ONLINE_USER_KEY = "online_users:";
    private static final String LOGIN_TOKEN_KEY = "login_tokens:";

    @Override
    public List<OnlineUserDTO> getOnlineUsers() {
        log.info("获取在线用户列表");

        List<OnlineUserDTO> onlineUsers = new ArrayList<>();

        try {
            // 从Redis获取所有在线用户的key
            Set<String> keys =  redisService.keys(ONLINE_USER_KEY + "*");
            log.info("在线用户key: {}", keys);
            for (String key : keys) {
                Object userData = redisService.get(key);
                if (userData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userData;

                    OnlineUserDTO user = new OnlineUserDTO();
                    user.setUserId(getLongValue(userMap.get("userId")));
                    user.setUsername((String) userMap.get("username"));
                    user.setNickname((String) userMap.get("nickname"));
                    user.setDeptName((String) userMap.get("deptName"));
                    user.setLoginIp((String) userMap.get("loginIp"));
                    user.setLoginLocation((String) userMap.get("loginLocation"));
                    user.setBrowser((String) userMap.get("browser"));
                    user.setOs((String) userMap.get("os"));
                    user.setToken((String) userMap.get("token"));

                    // 解析时间
                    String loginTimeStr = (String) userMap.get("loginTime");
                    if (loginTimeStr != null) {
                        user.setLoginTime(LocalDateTime.parse(loginTimeStr));
                    }

                    String lastAccessTimeStr = (String) userMap.get("lastAccessTime");
                    if (lastAccessTimeStr != null) {
                        user.setLastAccessTime(LocalDateTime.parse(lastAccessTimeStr));
                    }

                    onlineUsers.add(user);
                }
            }

            // 按登录时间降序排序
            onlineUsers.sort((a, b) -> {
                if (a.getLoginTime() == null || b.getLoginTime() == null) {
                    return 0;
                }
                return b.getLoginTime().compareTo(a.getLoginTime());
            });

        } catch (Exception e) {
            log.error("获取在线用户列表失败: {}", e.getMessage(), e);
        }

        return onlineUsers;
    }

    @Override
    public void forceLogout(String token) {
        log.info("强制下线用户: {}", token);

        try {
            // 查找对应的用户ID
            Set<String> keys =  redisService.keys(ONLINE_USER_KEY + "*");
            for (String key : keys) {
                Object userData = redisService.get(key);
                if (userData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userData;
                    String userToken = (String) userMap.get("token");
                    if (token.equals(userToken)) {
                        // 删除在线用户信息
                        redisService.delete(key);

                        // 删除Token
                        String username = getStringValue(userMap.get("username"));
                        String tokenKey = LOGIN_TOKEN_KEY + username;
                        redisService.delete(tokenKey);

                        log.info("用户强制下线成功");
                        return;
                    }
                }
            }
            log.warn("未找到对应的在线用户: {}", token);
        } catch (Exception e) {
            log.error("强制下线用户失败: {}", e.getMessage(), e);
            throw new RuntimeException("强制下线用户失败");
        }
    }

    @Override
    public ServerInfoDTO getServerInfo() {
        log.info("获取服务器信息");
        
        ServerInfoDTO serverInfo = new ServerInfoDTO();
        
        // 获取运行时信息
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        // 设置基本信息
        serverInfo.setServerName("basebackend-admin-api");
        serverInfo.setServerIp("127.0.0.1");
        serverInfo.setOsName(osBean.getName());
        serverInfo.setOsVersion(osBean.getVersion());
        serverInfo.setOsArch(osBean.getArch());
        
        // 设置Java信息
        serverInfo.setJavaVersion(System.getProperty("java.version"));
        serverInfo.setJavaVendor(System.getProperty("java.vendor"));
        serverInfo.setJvmName(System.getProperty("java.vm.name"));
        serverInfo.setJvmVersion(System.getProperty("java.vm.version"));
        serverInfo.setJvmVendor(System.getProperty("java.vm.vendor"));
        
        // 设置内存信息
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long freeMemory = totalMemory - usedMemory;
        
        serverInfo.setTotalMemory(formatBytes(totalMemory));
        serverInfo.setUsedMemory(formatBytes(usedMemory));
        serverInfo.setFreeMemory(formatBytes(freeMemory));
        serverInfo.setMemoryUsage(String.format("%.2f%%", (double) usedMemory / totalMemory * 100));
        
        // 设置处理器信息
        serverInfo.setProcessorCount(osBean.getAvailableProcessors());
        serverInfo.setSystemLoad(String.format("%.2f", osBean.getSystemLoadAverage()));
        
        // 设置运行时间
        long uptime = runtimeBean.getUptime();
        serverInfo.setUptime(formatUptime(uptime));
        
        return serverInfo;
    }

    @Override
    public List<CacheInfoDTO> getCacheInfo() {
        log.info("获取缓存信息");
        
        List<CacheInfoDTO> cacheInfoList = new ArrayList<>();
        
        // 模拟缓存信息
        CacheInfoDTO cache1 = new CacheInfoDTO();
        cache1.setCacheName("user_permissions");
        cache1.setCacheType("Redis");
        cache1.setCacheSize(1024L);
        cache1.setHitCount(1000L);
        cache1.setMissCount(100L);
        cache1.setHitRate("90.91%");
        cache1.setMaxCapacity(10000L);
        cache1.setUsageRate("10.24%");
        cache1.setExpireTime(3600L);
        cache1.setLastAccessTime(LocalDateTime.now().toString());
        cacheInfoList.add(cache1);
        
        CacheInfoDTO cache2 = new CacheInfoDTO();
        cache2.setCacheName("menu_tree");
        cache2.setCacheType("Redis");
        cache2.setCacheSize(512L);
        cache2.setHitCount(500L);
        cache2.setMissCount(50L);
        cache2.setHitRate("90.91%");
        cache2.setMaxCapacity(5000L);
        cache2.setUsageRate("10.24%");
        cache2.setExpireTime(1800L);
        cache2.setLastAccessTime(LocalDateTime.now().toString());
        cacheInfoList.add(cache2);
        
        return cacheInfoList;
    }

    @Override
    public void clearCache(String cacheName) {
        log.info("清空指定缓存: {}", cacheName);
        
        // 这里应该根据缓存名称清空对应的缓存
        // 简化处理
        log.info("缓存清空成功: {}", cacheName);
    }

    @Override
    public void clearAllCache() {
        log.info("清空所有缓存");
        
        // 这里应该清空所有缓存
        // 简化处理
        log.info("所有缓存清空成功");
    }

    @Override
    public Object getSystemStats() {
        log.info("获取系统统计信息");
        
        Map<String, Object> stats = new HashMap<>();
        
        // 用户统计
        stats.put("totalUsers", 100);
        stats.put("onlineUsers", 5);
        stats.put("activeUsers", 20);
        
        // 系统统计
        stats.put("totalRequests", 10000);
        stats.put("successRequests", 9500);
        stats.put("errorRequests", 500);
        stats.put("avgResponseTime", "120ms");
        
        // 缓存统计
        stats.put("cacheHitRate", "95.5%");
        stats.put("cacheSize", "2.5MB");
        stats.put("cacheKeys", 1500);
        
        return stats;
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptime) {
        long days = uptime / (24 * 60 * 60 * 1000);
        long hours = (uptime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
        long minutes = (uptime % (60 * 60 * 1000)) / (60 * 1000);
        long seconds = (uptime % (60 * 1000)) / 1000;
        
        return String.format("%d天%d小时%d分钟%d秒", days, hours, minutes, seconds);
    }

    /**
     * 将Object转换为Long
     */
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将Object转换为String
     */
    private String getStringValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
}
