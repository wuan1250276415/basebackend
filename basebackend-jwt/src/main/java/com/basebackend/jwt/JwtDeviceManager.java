package com.basebackend.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT 多设备管理器 — 管理用户的多设备登录状态
 * <p>
 * 优先使用 Redis（Hash: jwt:devices:{userId}），
 * 当 Redis 不可用时降级为内存 ConcurrentHashMap。
 */
@Slf4j
public class JwtDeviceManager {

    private static final String REDIS_KEY_PREFIX = "jwt:devices:";

    @Nullable
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxDevicesPerUser;

    /** 降级用内存存储：userId -> (deviceId -> DeviceSession JSON) */
    private final Map<Long, Map<String, String>> memoryStore = new ConcurrentHashMap<>();

    public JwtDeviceManager(@Nullable StringRedisTemplate redisTemplate,
                            int maxDevicesPerUser) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.maxDevicesPerUser = maxDevicesPerUser;
    }

    /**
     * 注册设备会话。如果设备数量超过限制，踢掉最早登录的设备。
     *
     * @param userId     用户ID
     * @param deviceInfo 设备信息
     * @param tokenJti   关联的 Token JTI
     */
    public void registerDevice(Long userId, DeviceInfo deviceInfo, String tokenJti) {
        DeviceSession session = DeviceSession.from(deviceInfo, tokenJti);
        String sessionJson = toJson(session);
        if (sessionJson == null) return;

        if (isRedisAvailable()) {
            try {
                String redisKey = REDIS_KEY_PREFIX + userId;
                redisTemplate.opsForHash().put(redisKey, deviceInfo.getDeviceId(), sessionJson);
                enforceMaxDevicesRedis(userId, redisKey);
                log.debug("Device registered in Redis: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
                return;
            } catch (Exception e) {
                log.warn("Redis device register failed, falling back to memory: {}", e.getMessage());
            }
        }

        // 降级到内存
        memoryStore.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .put(deviceInfo.getDeviceId(), sessionJson);
        enforceMaxDevicesMemory(userId);
        log.debug("Device registered in memory: userId={}, deviceId={}", userId, deviceInfo.getDeviceId());
    }

    /**
     * 移除指定设备（踢下线）
     */
    public void removeDevice(Long userId, String deviceId) {
        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForHash().delete(REDIS_KEY_PREFIX + userId, deviceId);
                log.debug("Device removed from Redis: userId={}, deviceId={}", userId, deviceId);
                return;
            } catch (Exception e) {
                log.warn("Redis device remove failed: {}", e.getMessage());
            }
        }
        Map<String, String> devices = memoryStore.get(userId);
        if (devices != null) {
            devices.remove(deviceId);
        }
    }

    /**
     * 移除用户所有设备（全部踢下线）
     */
    public void removeAllDevices(Long userId) {
        if (isRedisAvailable()) {
            try {
                redisTemplate.delete(REDIS_KEY_PREFIX + userId);
                log.debug("All devices removed from Redis: userId={}", userId);
                return;
            } catch (Exception e) {
                log.warn("Redis remove all devices failed: {}", e.getMessage());
            }
        }
        memoryStore.remove(userId);
    }

    /**
     * 踢下线除当前设备外的所有设备
     */
    public void removeAllDevicesExcept(Long userId, String exceptDeviceId) {
        List<DeviceSession> devices = getActiveDevices(userId);
        for (DeviceSession session : devices) {
            if (!session.getDeviceId().equals(exceptDeviceId)) {
                removeDevice(userId, session.getDeviceId());
            }
        }
    }

    /**
     * 获取用户所有活跃设备列表
     */
    public List<DeviceSession> getActiveDevices(Long userId) {
        if (isRedisAvailable()) {
            try {
                Map<Object, Object> entries = redisTemplate.opsForHash().entries(REDIS_KEY_PREFIX + userId);
                List<DeviceSession> sessions = new ArrayList<>(entries.size());
                for (Object value : entries.values()) {
                    DeviceSession session = fromJson((String) value);
                    if (session != null) {
                        sessions.add(session);
                    }
                }
                return sessions;
            } catch (Exception e) {
                log.warn("Redis get active devices failed: {}", e.getMessage());
            }
        }
        Map<String, String> devices = memoryStore.get(userId);
        if (devices == null) {
            return List.of();
        }
        List<DeviceSession> sessions = new ArrayList<>(devices.size());
        for (String json : devices.values()) {
            DeviceSession session = fromJson(json);
            if (session != null) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    /**
     * 获取指定设备的会话信息
     */
    @Nullable
    public DeviceSession getDeviceSession(Long userId, String deviceId) {
        if (isRedisAvailable()) {
            try {
                Object value = redisTemplate.opsForHash().get(REDIS_KEY_PREFIX + userId, deviceId);
                return value != null ? fromJson((String) value) : null;
            } catch (Exception e) {
                log.warn("Redis get device session failed: {}", e.getMessage());
            }
        }
        Map<String, String> devices = memoryStore.get(userId);
        if (devices == null) return null;
        String json = devices.get(deviceId);
        return json != null ? fromJson(json) : null;
    }

    /**
     * 检查设备是否活跃
     */
    public boolean isDeviceActive(Long userId, String deviceId) {
        return getDeviceSession(userId, deviceId) != null;
    }

    /**
     * 更新最后活跃时间
     */
    public void updateLastActive(Long userId, String deviceId) {
        DeviceSession session = getDeviceSession(userId, deviceId);
        if (session == null) return;
        session.setLastActiveTime(System.currentTimeMillis());
        String sessionJson = toJson(session);
        if (sessionJson == null) return;

        if (isRedisAvailable()) {
            try {
                redisTemplate.opsForHash().put(REDIS_KEY_PREFIX + userId, deviceId, sessionJson);
                return;
            } catch (Exception e) {
                log.warn("Redis update last active failed: {}", e.getMessage());
            }
        }
        Map<String, String> devices = memoryStore.get(userId);
        if (devices != null) {
            devices.put(deviceId, sessionJson);
        }
    }

    // ========== 内部方法 ==========

    private void enforceMaxDevicesRedis(Long userId, String redisKey) {
        if (maxDevicesPerUser <= 0) return;
        try {
            Long size = redisTemplate.opsForHash().size(redisKey);
            if (size != null && size > maxDevicesPerUser) {
                List<DeviceSession> allDevices = getActiveDevices(userId);
                allDevices.sort(Comparator.comparingLong(DeviceSession::getLoginTime));
                int toRemove = allDevices.size() - maxDevicesPerUser;
                for (int i = 0; i < toRemove; i++) {
                    String oldDeviceId = allDevices.get(i).getDeviceId();
                    redisTemplate.opsForHash().delete(redisKey, oldDeviceId);
                    log.info("Device evicted due to max-devices limit: userId={}, deviceId={}",
                            userId, oldDeviceId);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to enforce max devices in Redis: {}", e.getMessage());
        }
    }

    private void enforceMaxDevicesMemory(Long userId) {
        if (maxDevicesPerUser <= 0) return;
        Map<String, String> devices = memoryStore.get(userId);
        if (devices == null || devices.size() <= maxDevicesPerUser) return;

        List<DeviceSession> allDevices = new ArrayList<>();
        for (String json : devices.values()) {
            DeviceSession session = fromJson(json);
            if (session != null) {
                allDevices.add(session);
            }
        }
        allDevices.sort(Comparator.comparingLong(DeviceSession::getLoginTime));
        int toRemove = allDevices.size() - maxDevicesPerUser;
        for (int i = 0; i < toRemove; i++) {
            String oldDeviceId = allDevices.get(i).getDeviceId();
            devices.remove(oldDeviceId);
            log.info("Device evicted due to max-devices limit: userId={}, deviceId={}",
                    userId, oldDeviceId);
        }
    }

    private boolean isRedisAvailable() {
        return redisTemplate != null;
    }

    @Nullable
    private String toJson(DeviceSession session) {
        try {
            return objectMapper.writeValueAsString(session);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DeviceSession: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    private DeviceSession fromJson(String json) {
        try {
            return objectMapper.readValue(json, DeviceSession.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize DeviceSession: {}", e.getMessage());
            return null;
        }
    }
}
