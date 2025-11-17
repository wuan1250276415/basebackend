package com.basebackend.monitor.service;

import com.basebackend.monitor.dto.CacheInfoDTO;
import com.basebackend.monitor.dto.OnlineUserDTO;
import com.basebackend.monitor.dto.ServerInfoDTO;

import java.util.List;

/**
 * 系统监控服务接口
 *
 * @author BaseBackend Team
 * @since 2025-11-14
 */
public interface MonitorService {

    /**
     * 获取在线用户列表
     *
     * @return 在线用户列表
     */
    List<OnlineUserDTO> getOnlineUsers();

    /**
     * 强制用户下线
     *
     * @param token 用户令牌
     */
    void forceLogout(String token);

    /**
     * 获取服务器信息
     *
     * @return 服务器信息
     */
    ServerInfoDTO getServerInfo();

    /**
     * 获取缓存信息
     *
     * @return 缓存信息列表
     */
    List<CacheInfoDTO> getCacheInfo();

    /**
     * 清空指定缓存
     *
     * @param cacheName 缓存名称
     */
    void clearCache(String cacheName);

    /**
     * 清空所有缓存
     */
    void clearAllCache();

    /**
     * 获取系统统计信息
     *
     * @return 系统统计信息
     */
    Object getSystemStats();
}
