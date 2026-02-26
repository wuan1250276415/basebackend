package com.basebackend.websocket.channel;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 频道/房间管理器
 * <p>
 * 管理 WebSocket 的频道（Channel）和房间（Room）模型，支持：
 * <ul>
 *   <li>用户加入/离开频道</li>
 *   <li>频道级消息推送</li>
 *   <li>频道成员查询</li>
 * </ul>
 */
@Slf4j
public class ChannelManager {

    /** channelId → Set<userId> */
    private final Map<String, Set<String>> channels = new ConcurrentHashMap<>();

    /** userId → Set<channelId> (反向索引) */
    private final Map<String, Set<String>> userChannels = new ConcurrentHashMap<>();

    /**
     * 用户加入频道
     */
    public void join(String channelId, String userId) {
        channels.computeIfAbsent(channelId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        userChannels.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(channelId);
        log.info("用户加入频道: userId={}, channelId={}, 频道人数={}", userId, channelId, getMembers(channelId).size());
    }

    /**
     * 用户离开频道
     */
    public void leave(String channelId, String userId) {
        Set<String> members = channels.get(channelId);
        if (members != null) {
            members.remove(userId);
            if (members.isEmpty()) {
                channels.remove(channelId);
            }
        }

        Set<String> joined = userChannels.get(userId);
        if (joined != null) {
            joined.remove(channelId);
            if (joined.isEmpty()) {
                userChannels.remove(userId);
            }
        }

        log.info("用户离开频道: userId={}, channelId={}", userId, channelId);
    }

    /**
     * 用户离开所有频道（断线时调用）
     */
    public void leaveAll(String userId) {
        Set<String> joined = userChannels.remove(userId);
        if (joined != null) {
            for (String channelId : joined) {
                Set<String> members = channels.get(channelId);
                if (members != null) {
                    members.remove(userId);
                    if (members.isEmpty()) {
                        channels.remove(channelId);
                    }
                }
            }
            log.info("用户离开所有频道: userId={}, 频道数={}", userId, joined.size());
        }
    }

    /**
     * 获取频道成员
     */
    public Set<String> getMembers(String channelId) {
        Set<String> members = channels.get(channelId);
        return members != null ? Set.copyOf(members) : Set.of();
    }

    /**
     * 获取用户加入的所有频道
     */
    public Set<String> getUserChannels(String userId) {
        Set<String> joined = userChannels.get(userId);
        return joined != null ? Set.copyOf(joined) : Set.of();
    }

    /**
     * 频道是否存在
     */
    public boolean exists(String channelId) {
        return channels.containsKey(channelId);
    }

    /**
     * 用户是否在频道中
     */
    public boolean isMember(String channelId, String userId) {
        Set<String> members = channels.get(channelId);
        return members != null && members.contains(userId);
    }

    /**
     * 获取所有频道 ID
     */
    public Set<String> getAllChannelIds() {
        return Set.copyOf(channels.keySet());
    }

    /**
     * 获取频道数量
     */
    public int getChannelCount() {
        return channels.size();
    }

    /**
     * 获取频道成员数
     */
    public int getMemberCount(String channelId) {
        Set<String> members = channels.get(channelId);
        return members != null ? members.size() : 0;
    }
}
