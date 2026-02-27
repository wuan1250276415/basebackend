package com.basebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.chat.entity.ChatFriend;
import com.basebackend.chat.mapper.ChatFriendMapper;
import com.basebackend.chat.service.OnlineStatusService;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线状态服务实现
 * <p>
 * Redis 存储结构:
 * <ul>
 *   <li>{@code chat:online:{tenantId}} — SET — 在线用户ID集合</li>
 *   <li>{@code chat:status:{tenantId}:{userId}} — STRING — 用户状态 (online/offline/busy)</li>
 *   <li>{@code chat:status:last_active:{tenantId}:{userId}} — STRING — 最后活跃时间戳, TTL 7天</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusServiceImpl implements OnlineStatusService {

    private final StringRedisTemplate redisTemplate;
    private final SessionManager sessionManager;
    private final ChatFriendMapper friendMapper;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void userOnline(Long tenantId, Long userId) {
        String userIdStr = String.valueOf(userId);
        String tenantIdStr = String.valueOf(tenantId);

        redisTemplate.opsForSet().add("chat:online:" + tenantIdStr, userIdStr);
        redisTemplate.opsForValue().set("chat:status:" + tenantIdStr + ":" + userIdStr, "online");

        // 广播上线事件给好友
        broadcastPresenceToFriends(tenantId, userId, "online");

        log.info("用户上线: tenantId={}, userId={}", tenantId, userId);
    }

    @Override
    public void userOffline(Long tenantId, Long userId, boolean hasOtherSessions) {
        if (hasOtherSessions) {
            log.debug("用户仍有其他活跃会话，跳过离线标记: userId={}", userId);
            return;
        }

        String userIdStr = String.valueOf(userId);
        String tenantIdStr = String.valueOf(tenantId);

        redisTemplate.opsForSet().remove("chat:online:" + tenantIdStr, userIdStr);
        redisTemplate.opsForValue().set("chat:status:" + tenantIdStr + ":" + userIdStr, "offline");
        redisTemplate.opsForValue().set(
                "chat:status:last_active:" + tenantIdStr + ":" + userIdStr,
                String.valueOf(Instant.now().toEpochMilli()),
                Duration.ofDays(7)
        );

        // 广播离线事件给好友
        broadcastPresenceToFriends(tenantId, userId, "offline");

        log.info("用户离线: tenantId={}, userId={}", tenantId, userId);
    }

    @Override
    public void setStatus(Long tenantId, Long userId, String status) {
        String key = "chat:status:" + tenantId + ":" + userId;
        redisTemplate.opsForValue().set(key, status);

        // 广播状态变更给好友
        broadcastPresenceToFriends(tenantId, userId, status);
    }

    @Override
    public String getStatus(Long tenantId, Long userId) {
        String key = "chat:status:" + tenantId + ":" + userId;
        String status = redisTemplate.opsForValue().get(key);
        return status != null ? status : "offline";
    }

    @Override
    public Map<String, Map<String, Object>> batchGetStatus(Long tenantId, List<Long> userIds) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        String tenantIdStr = String.valueOf(tenantId);

        for (Long userId : userIds) {
            String userIdStr = String.valueOf(userId);
            String statusKey = "chat:status:" + tenantIdStr + ":" + userIdStr;
            String lastActiveKey = "chat:status:last_active:" + tenantIdStr + ":" + userIdStr;

            String status = redisTemplate.opsForValue().get(statusKey);
            String lastActiveMs = redisTemplate.opsForValue().get(lastActiveKey);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", status != null ? status : "offline");

            if (lastActiveMs != null) {
                try {
                    long ms = Long.parseLong(lastActiveMs);
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
                    entry.put("lastActive", ldt.format(DT_FMT));
                } catch (NumberFormatException e) {
                    entry.put("lastActive", null);
                }
            } else {
                entry.put("lastActive", null);
            }

            result.put(userIdStr, entry);
        }

        return result;
    }

    @Override
    public boolean isOnline(Long tenantId, Long userId) {
        Boolean member = redisTemplate.opsForSet()
                .isMember("chat:online:" + tenantId, String.valueOf(userId));
        return Boolean.TRUE.equals(member);
    }

    // ======================== 内部方法 ========================

    /**
     * 广播在线状态变更事件给用户的所有好友
     */
    private void broadcastPresenceToFriends(Long tenantId, Long userId, String status) {
        try {
            // 查询当前用户的好友列表
            List<ChatFriend> friends = friendMapper.selectList(
                    new LambdaQueryWrapper<ChatFriend>()
                            .eq(ChatFriend::getTenantId, tenantId)
                            .eq(ChatFriend::getUserId, userId)
                            .eq(ChatFriend::getStatus, 1)
            );

            if (friends.isEmpty()) return;

            String payload = JsonUtils.toJsonString(Map.of(
                    "type", "presence",
                    "userId", userId,
                    "status", status,
                    "timestamp", Instant.now().toEpochMilli()
            ));

            for (ChatFriend friend : friends) {
                sessionManager.sendToUser(String.valueOf(friend.getFriendId()), payload);
            }
        } catch (Exception e) {
            log.warn("广播在线状态失败: userId={}, error={}", userId, e.getMessage());
        }
    }
}
