package com.basebackend.chat.service;

import com.basebackend.chat.dto.request.CreateGroupRequest;
import com.basebackend.chat.dto.request.UpdateGroupRequest;
import com.basebackend.chat.dto.response.GroupMemberVO;
import com.basebackend.chat.dto.response.GroupVO;

import java.util.List;
import java.util.Map;

/**
 * 群组服务接口
 */
public interface ChatGroupService {

    /**
     * 创建群
     */
    Map<String, Object> createGroup(Long currentUserId, Long tenantId, CreateGroupRequest request);

    /**
     * 获取群信息
     */
    GroupVO getGroupInfo(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 修改群信息
     */
    void updateGroup(Long currentUserId, Long tenantId, Long groupId, UpdateGroupRequest request);

    /**
     * 解散群（仅群主）
     */
    void dissolveGroup(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 获取群成员列表
     */
    List<GroupMemberVO> listMembers(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 邀请入群
     */
    void inviteMembers(Long currentUserId, Long tenantId, Long groupId, List<Long> userIds);

    /**
     * 踢出成员
     */
    void kickMember(Long currentUserId, Long tenantId, Long groupId, Long userId);

    /**
     * 退出群聊
     */
    void leaveGroup(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 设置成员角色
     */
    void setMemberRole(Long currentUserId, Long tenantId, Long groupId, Long userId, Integer role);

    /**
     * 禁言/解禁成员
     */
    void muteMember(Long currentUserId, Long tenantId, Long groupId, Long userId,
                    boolean isMuted, Integer duration);

    /**
     * 全体禁言/解禁
     */
    void muteAll(Long currentUserId, Long tenantId, Long groupId, boolean isMuted);

    /**
     * 修改群内昵称
     */
    void updateNickname(Long currentUserId, Long tenantId, Long groupId, String nickname);

    /**
     * 转让群主
     */
    void transferOwner(Long currentUserId, Long tenantId, Long groupId, Long newOwnerId);

    /**
     * 获取群公告列表
     */
    List<Map<String, Object>> listAnnouncements(Long currentUserId, Long tenantId, Long groupId);

    /**
     * 发布群公告
     */
    Map<String, Object> createAnnouncement(Long currentUserId, Long tenantId, Long groupId,
                                           String title, String content, Boolean isPinned);

    /**
     * 编辑群公告
     */
    void updateAnnouncement(Long currentUserId, Long tenantId, Long groupId, Long announcementId,
                            String title, String content, Boolean isPinned);

    /**
     * 删除群公告
     */
    void deleteAnnouncement(Long currentUserId, Long tenantId, Long groupId, Long announcementId);
}
