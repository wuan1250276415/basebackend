package com.basebackend.admin.service;

import com.basebackend.admin.dto.CacheInfoDTO;
import com.basebackend.admin.dto.OnlineUserDTO;
import com.basebackend.admin.dto.ServerInfoDTO;

import java.util.List;

/**
 * 系统监控服务接口
 */
public interface MonitorService {

    /**
     * 获取在线用户列表
     */
    List<OnlineUserDTO> getOnlineUsers();

    /**
     * 强制下线用户
     */
    void forceLogout(String token);

    /**
     * 获取服务器信息
     */
    ServerInfoDTO getServerInfo();

    /**
     * 获取缓存信息
     */
    List<CacheInfoDTO> getCacheInfo();

    /**
     * 清空指定缓存
     */
    void clearCache(String cacheName);

    /**
     * 清空所有缓存
     */
    void clearAllCache();

    /**
     * 获取系统统计信息
     */
    Object getSystemStats();
}
