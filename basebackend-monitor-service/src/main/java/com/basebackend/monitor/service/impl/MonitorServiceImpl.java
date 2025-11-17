package com.basebackend.monitor.service.impl;

import com.basebackend.cache.service.RedisService;
import com.basebackend.monitor.dto.CacheInfoDTO;
import com.basebackend.monitor.dto.OnlineUserDTO;
import com.basebackend.monitor.dto.ServerInfoDTO;
import com.basebackend.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 系统监控服务实现
 *
 * @author BaseBackend Team
 * @since 2025-11-14
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
            // 从 Redis 获取所有在线用户
            Set<String> keys = redisService.keys(ONLINE_USER_KEY + "*");
            log.debug("找到 {} 个在线用户会话", keys.size());

            for (String key : keys) {
                Object userData = redisService.get(key);
                if (userData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userData;

                    OnlineUserDTO user = new OnlineUserDTO();
                    user.setUserId(getLongValue(userMap.get("userId")));
                    user.setUsername(getStringValue(userMap.get("username")));
                    user.setNickname(getStringValue(userMap.get("nickname")));
                    user.setDeptName(getStringValue(userMap.get("deptName")));
                    user.setLoginIp(getStringValue(userMap.get("loginIp")));
                    user.setLoginLocation(getStringValue(userMap.get("loginLocation")));
                    user.setBrowser(getStringValue(userMap.get("browser")));
                    user.setOs(getStringValue(userMap.get("os")));
                    user.setToken(getStringValue(userMap.get("token")));

                    // 处理时间戳
                    Object loginTime = userMap.get("loginTime");
                    if (loginTime != null) {
                        user.setLoginTime(parseDateTime(loginTime));
                    }

                    Object lastAccessTime = userMap.get("lastAccessTime");
                    if (lastAccessTime != null) {
                        user.setLastAccessTime(parseDateTime(lastAccessTime));
                    }

                    onlineUsers.add(user);
                }
            }

            // 按登录时间倒序排序
            onlineUsers.sort((a, b) -> {
                if (a.getLoginTime() == null && b.getLoginTime() == null) return 0;
                if (a.getLoginTime() == null) return 1;
                if (b.getLoginTime() == null) return -1;
                return b.getLoginTime().compareTo(a.getLoginTime());
            });

        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
        }

        return onlineUsers;
    }

    @Override
    public void forceLogout(String token) {
        log.info("强制用户下线: token={}", token);

        try {
            Set<String> keys = redisService.keys(ONLINE_USER_KEY + "*");

            for (String key : keys) {
                Object userData = redisService.get(key);
                if (userData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> userMap = (Map<String, Object>) userData;
                    String userToken = getStringValue(userMap.get("token"));

                    if (token.equals(userToken)) {
                        // 删除在线用户信息
                        redisService.delete(key);

                        // 删除登录令牌
                        String username = getStringValue(userMap.get("username"));
                        if (username != null) {
                            String tokenKey = LOGIN_TOKEN_KEY + username;
                            redisService.delete(tokenKey);
                        }

                        log.info("用户已下线: username={}", username);
                        return;
                    }
                }
            }

            log.warn("未找到匹配的用户: token={}", token);
        } catch (Exception e) {
            log.error("强制用户下线失败", e);
            throw new RuntimeException("强制下线失败: " + e.getMessage());
        }
    }

    @Override
    public ServerInfoDTO getServerInfo() {
        log.info("获取服务器信息");
        ServerInfoDTO serverInfo = new ServerInfoDTO();

        try {
            // 获取 JVM 运行时信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

            // 服务器基本信息
            serverInfo.setServerName("basebackend-monitor-service");
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                serverInfo.setServerIp(localHost.getHostAddress());
            } catch (Exception e) {
                serverInfo.setServerIp("Unknown");
            }

            // 操作系统信息
            serverInfo.setOsName(osBean.getName());
            serverInfo.setOsVersion(osBean.getVersion());
            serverInfo.setOsArch(osBean.getArch());

            // Java 信息
            serverInfo.setJavaVersion(System.getProperty("java.version"));
            serverInfo.setJavaVendor(System.getProperty("java.vendor"));
            serverInfo.setJvmName(runtimeBean.getVmName());
            serverInfo.setJvmVersion(runtimeBean.getVmVersion());
            serverInfo.setJvmVendor(runtimeBean.getVmVendor());

            // 内存信息
            long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long freeMemory = totalMemory - usedMemory;

            serverInfo.setTotalMemory(formatBytes(totalMemory));
            serverInfo.setUsedMemory(formatBytes(usedMemory));
            serverInfo.setFreeMemory(formatBytes(freeMemory));

            if (totalMemory > 0) {
                double usage = (double) usedMemory / totalMemory * 100;
                serverInfo.setMemoryUsage(String.format("%.2f%%", usage));
            } else {
                serverInfo.setMemoryUsage("N/A");
            }

            // 处理器信息
            serverInfo.setProcessorCount(osBean.getAvailableProcessors());

            // 系统负载（需要特定的 MXBean）
            try {
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                    double systemLoad = sunOsBean.getSystemCpuLoad() * 100;
                    serverInfo.setSystemLoad(String.format("%.2f%%", systemLoad));
                } else {
                    serverInfo.setSystemLoad("N/A");
                }
            } catch (Exception e) {
                serverInfo.setSystemLoad("N/A");
            }

            // 运行时间
            long uptime = runtimeBean.getUptime();
            serverInfo.setUptime(formatUptime(uptime));

        } catch (Exception e) {
            log.error("获取服务器信息失败", e);
        }

        return serverInfo;
    }

    @Override
    public List<CacheInfoDTO> getCacheInfo() {
        log.info("获取缓存信息");
        List<CacheInfoDTO> cacheInfoList = new ArrayList<>();

        try {
            // TODO: 实现真实的缓存信息统计
            // 目前返回模拟数据

            CacheInfoDTO cache1 = new CacheInfoDTO();
            cache1.setCacheName("user_permissions");
            cache1.setCacheType("Redis");
            cache1.setCacheSize(1000L);
            cache1.setHitCount(9091L);
            cache1.setMissCount(909L);
            cache1.setHitRate("90.91%");
            cache1.setMaxCapacity(10000L);
            cache1.setUsageRate("10.00%");
            cache1.setExpireTime(3600L);
            cache1.setLastAccessTime(LocalDateTime.now().toString());
            cacheInfoList.add(cache1);

            CacheInfoDTO cache2 = new CacheInfoDTO();
            cache2.setCacheName("online_users");
            cache2.setCacheType("Redis");
            cache2.setCacheSize(50L);
            cache2.setHitCount(8500L);
            cache2.setMissCount(500L);
            cache2.setHitRate("94.44%");
            cache2.setMaxCapacity(1000L);
            cache2.setUsageRate("5.00%");
            cache2.setExpireTime(1800L);
            cache2.setLastAccessTime(LocalDateTime.now().toString());
            cacheInfoList.add(cache2);

        } catch (Exception e) {
            log.error("获取缓存信息失败", e);
        }

        return cacheInfoList;
    }

    @Override
    public void clearCache(String cacheName) {
        log.info("清空缓存: cacheName={}", cacheName);

        try {
            // 根据缓存名称清空对应的 Redis keys
            Set<String> keys = redisService.keys(cacheName + "*");
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
                log.info("缓存已清空: cacheName={}, keys={}", cacheName, keys.size());
            } else {
                log.warn("未找到匹配的缓存: cacheName={}", cacheName);
            }
        } catch (Exception e) {
            log.error("清空缓存失败: cacheName={}", cacheName, e);
            throw new RuntimeException("清空缓存失败: " + e.getMessage());
        }
    }

    @Override
    public void clearAllCache() {
        log.info("清空所有缓存");

        try {
            // 清空所有 Redis 缓存
            // 注意：这是一个危险操作，生产环境应该谨慎使用
            Set<String> keys = redisService.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisService.delete(keys);
                log.info("所有缓存已清空: keys={}", keys.size());
            }
        } catch (Exception e) {
            log.error("清空所有缓存失败", e);
            throw new RuntimeException("清空所有缓存失败: " + e.getMessage());
        }
    }

    @Override
    public Object getSystemStats() {
        log.info("获取系统统计信息");

        try {
            Map<String, Object> stats = new HashMap<>();

            // 在线用户统计
            Set<String> onlineUserKeys = redisService.keys(ONLINE_USER_KEY + "*");
            stats.put("onlineUsers", onlineUserKeys.size());

            // 系统资源统计
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            stats.put("memoryUsage", String.format("%.2f%%", (double) usedMemory / totalMemory * 100));

            // 缓存命中率（模拟数据）
            stats.put("cacheHitRate", "95.5%");

            // 运行时间
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            stats.put("uptime", formatUptime(runtimeBean.getUptime()));

            return stats;
        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            return Collections.emptyMap();
        }
    }

    /**
     * 格式化字节大小
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "N/A";
        } else if (bytes < 1024) {
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

        if (days > 0) {
            return String.format("%d天%d小时%d分钟%d秒", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%d小时%d分钟%d秒", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 从 Map 中获取 Long 值
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
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从 Map 中获取 String 值
     */
    private String getStringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * 解析日期时间
     */
    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
            if (value instanceof Long) {
                return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli((Long) value),
                    ZoneId.systemDefault()
                );
            }
            if (value instanceof String) {
                return LocalDateTime.parse((String) value);
            }
        } catch (Exception e) {
            log.warn("解析日期时间失败: value={}", value, e);
        }

        return null;
    }
}
