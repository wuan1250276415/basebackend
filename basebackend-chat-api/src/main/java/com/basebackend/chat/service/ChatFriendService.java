package com.basebackend.chat.service;

import com.basebackend.chat.dto.request.FriendRequestDTO;
import com.basebackend.chat.dto.request.HandleFriendRequestDTO;
import com.basebackend.chat.dto.response.FriendVO;
import com.basebackend.common.dto.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 好友服务接口
 */
public interface ChatFriendService {

    /**
     * 搜索用户（按用户名/手机号/邮箱）
     */
    List<Map<String, Object>> searchUsers(Long currentUserId, Long tenantId, String keyword);

    /**
     * 发送好友申请
     */
    Map<String, Object> sendFriendRequest(Long currentUserId, Long tenantId, FriendRequestDTO request);

    /**
     * 处理好友申请（同意/拒绝）
     */
    void handleFriendRequest(Long currentUserId, Long tenantId, Long requestId,
                             HandleFriendRequestDTO request);

    /**
     * 获取好友申请列表
     */
    PageResult<Map<String, Object>> listFriendRequests(Long currentUserId, Long tenantId,
                                                       Integer pageNum, Integer pageSize);

    /**
     * 获取好友列表
     */
    List<FriendVO> listFriends(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 修改好友备注
     */
    void updateRemark(Long currentUserId, Long tenantId, Long friendUserId, String remark);

    /**
     * 删除好友
     */
    void deleteFriend(Long currentUserId, Long tenantId, Long friendUserId);

    /**
     * 好友分组列表
     */
    List<Map<String, Object>> listFriendGroups(Long currentUserId, Long tenantId);

    /**
     * 创建好友分组
     */
    Map<String, Object> createFriendGroup(Long currentUserId, Long tenantId,
                                          String name, Integer sortOrder);

    /**
     * 修改好友分组
     */
    void updateFriendGroup(Long currentUserId, Long tenantId, Long groupId,
                           String name, Integer sortOrder);

    /**
     * 删除好友分组（好友移至默认组）
     */
    void deleteFriendGroup(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 移动好友到指定分组
     */
    void moveFriendToGroup(Long currentUserId, Long tenantId, Long friendUserId, Long groupId);

    /**
     * 拉黑用户
     */
    void blockUser(Long currentUserId, Long tenantId, Long blockedId, String reason);

    /**
     * 取消拉黑
     */
    void unblockUser(Long currentUserId, Long tenantId, Long blockedUserId);

    /**
     * 获取黑名单列表
     */
    List<Map<String, Object>> listBlacklist(Long currentUserId, Long tenantId);
}
