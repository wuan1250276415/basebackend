package com.basebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.chat.dto.request.CreateGroupRequest;
import com.basebackend.chat.dto.request.UpdateGroupRequest;
import com.basebackend.chat.dto.response.GroupMemberVO;
import com.basebackend.chat.dto.response.GroupVO;
import com.basebackend.chat.entity.ChatConversation;
import com.basebackend.chat.entity.ChatConversationMember;
import com.basebackend.chat.entity.ChatGroup;
import com.basebackend.chat.entity.ChatGroupAnnouncement;
import com.basebackend.chat.entity.ChatGroupMember;
import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.chat.enums.ConversationType;
import com.basebackend.chat.enums.GroupRole;
import com.basebackend.chat.enums.GroupStatus;
import com.basebackend.chat.mapper.ChatConversationMapper;
import com.basebackend.chat.mapper.ChatConversationMemberMapper;
import com.basebackend.chat.mapper.ChatGroupAnnouncementMapper;
import com.basebackend.chat.mapper.ChatGroupMapper;
import com.basebackend.chat.mapper.ChatGroupMemberMapper;
import com.basebackend.chat.service.ChatGroupService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 群组服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGroupServiceImpl implements ChatGroupService {

    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper groupMemberMapper;
    private final ChatGroupAnnouncementMapper announcementMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper conversationMemberMapper;
    private final SessionManager sessionManager;

    @Override
    @Transactional
    public Map<String, Object> createGroup(Long currentUserId, Long tenantId,
                                           CreateGroupRequest request) {
        // 创建群会话
        ChatConversation conversation = new ChatConversation();
        conversation.setTenantId(tenantId);
        conversation.setType(ConversationType.GROUP.getCode());
        conversation.setTargetId(0L); // 占位，后续用群ID回填
        int memberCount = 1 + (request.getMemberIds() != null ? request.getMemberIds().size() : 0);
        conversation.setMemberCount(memberCount);
        conversationMapper.insert(conversation);

        // 创建群
        ChatGroup group = new ChatGroup();
        group.setTenantId(tenantId);
        group.setName(request.getName());
        group.setAvatar(request.getAvatar());
        group.setDescription(request.getDescription());
        group.setOwnerId(currentUserId);
        group.setConversationId(conversation.getId());
        group.setMemberCount(memberCount);
        group.setStatus(GroupStatus.NORMAL.getCode());
        groupMapper.insert(group);

        // 回填会话的 targetId
        conversationMapper.update(null,
                new LambdaUpdateWrapper<ChatConversation>()
                        .eq(ChatConversation::getId, conversation.getId())
                        .set(ChatConversation::getTargetId, group.getId())
        );

        // 添加群主为成员
        addGroupMember(tenantId, group.getId(), currentUserId, GroupRole.OWNER.getCode(), null);
        addConversationMember(tenantId, conversation.getId(), currentUserId);

        // 添加初始成员
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                addGroupMember(tenantId, group.getId(), memberId, GroupRole.MEMBER.getCode(), currentUserId);
                addConversationMember(tenantId, conversation.getId(), memberId);
            }
        }

        return Map.of(
                "groupId", group.getId(),
                "conversationId", conversation.getId(),
                "name", group.getName(),
                "memberCount", memberCount
        );
    }

    @Override
    public GroupVO getGroupInfo(Long currentUserId, Long tenantId, Long groupId) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);

        // 查询当前用户在群中的角色
        ChatGroupMember myMembership = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, currentUserId)
        );

        return GroupVO.builder()
                .groupId(group.getId())
                .name(group.getName())
                .avatar(group.getAvatar())
                .description(group.getDescription())
                .ownerId(group.getOwnerId())
                .conversationId(group.getConversationId())
                .maxMembers(group.getMaxMembers())
                .memberCount(group.getMemberCount())
                .isMuted(group.getIsMuted() != null && group.getIsMuted() == 1)
                .joinMode(group.getJoinMode())
                .inviteConfirm(group.getInviteConfirm() != null && group.getInviteConfirm() == 1)
                .myRole(myMembership != null ? myMembership.getRole() : null)
                .createTime(group.getCreateTime())
                .build();
    }

    @Override
    public void updateGroup(Long currentUserId, Long tenantId, Long groupId,
                            UpdateGroupRequest request) {
        verifyAdminPermission(tenantId, groupId, currentUserId);

        LambdaUpdateWrapper<ChatGroup> wrapper = new LambdaUpdateWrapper<ChatGroup>()
                .eq(ChatGroup::getId, groupId)
                .eq(ChatGroup::getTenantId, tenantId);
        if (request.getName() != null) wrapper.set(ChatGroup::getName, request.getName());
        if (request.getAvatar() != null) wrapper.set(ChatGroup::getAvatar, request.getAvatar());
        if (request.getDescription() != null) wrapper.set(ChatGroup::getDescription, request.getDescription());
        if (request.getJoinMode() != null) wrapper.set(ChatGroup::getJoinMode, request.getJoinMode());
        if (request.getInviteConfirm() != null) {
            wrapper.set(ChatGroup::getInviteConfirm, request.getInviteConfirm() ? 1 : 0);
        }
        groupMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void dissolveGroup(Long currentUserId, Long tenantId, Long groupId) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (!currentUserId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.GROUP_PERMISSION_DENIED, "仅群主可解散群");
        }

        groupMapper.update(null,
                new LambdaUpdateWrapper<ChatGroup>()
                        .eq(ChatGroup::getId, groupId)
                        .set(ChatGroup::getStatus, GroupStatus.DISSOLVED.getCode())
        );
    }

    @Override
    public List<GroupMemberVO> listMembers(Long currentUserId, Long tenantId, Long groupId) {
        verifyGroupMember(tenantId, groupId, currentUserId);

        List<ChatGroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .orderByDesc(ChatGroupMember::getRole)
                        .orderByAsc(ChatGroupMember::getJoinTime)
        );

        return members.stream()
                .map(m -> GroupMemberVO.builder()
                        .userId(m.getUserId())
                        .groupNickname(m.getNickname())
                        .role(m.getRole())
                        .isMuted(m.getIsMuted() != null && m.getIsMuted() == 1)
                        .joinTime(m.getJoinTime())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void inviteMembers(Long currentUserId, Long tenantId, Long groupId, List<Long> userIds) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        verifyGroupMember(tenantId, groupId, currentUserId);

        if (group.getMemberCount() + userIds.size() > group.getMaxMembers()) {
            throw new BusinessException(ChatErrorCode.GROUP_MEMBER_FULL);
        }

        for (Long userId : userIds) {
            // 检查是否已是成员
            Long count = groupMemberMapper.selectCount(
                    new LambdaQueryWrapper<ChatGroupMember>()
                            .eq(ChatGroupMember::getTenantId, tenantId)
                            .eq(ChatGroupMember::getGroupId, groupId)
                            .eq(ChatGroupMember::getUserId, userId)
            );
            if (count != null && count > 0) continue;

            addGroupMember(tenantId, groupId, userId, GroupRole.MEMBER.getCode(), currentUserId);
            if (group.getConversationId() != null) {
                addConversationMember(tenantId, group.getConversationId(), userId);
            }
        }

        // 更新群成员数
        updateGroupMemberCount(tenantId, groupId);
    }

    @Override
    @Transactional
    public void kickMember(Long currentUserId, Long tenantId, Long groupId, Long userId) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (userId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.CANNOT_OPERATE_OWNER);
        }
        verifyAdminPermission(tenantId, groupId, currentUserId);

        groupMemberMapper.delete(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );

        if (group.getConversationId() != null) {
            conversationMemberMapper.delete(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getTenantId, tenantId)
                            .eq(ChatConversationMember::getConversationId, group.getConversationId())
                            .eq(ChatConversationMember::getUserId, userId)
            );
        }

        updateGroupMemberCount(tenantId, groupId);
    }

    @Override
    @Transactional
    public void leaveGroup(Long currentUserId, Long tenantId, Long groupId) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (currentUserId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.CANNOT_OPERATE_OWNER, "群主不能退出群聊，请先转让群主");
        }

        groupMemberMapper.delete(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, currentUserId)
        );

        if (group.getConversationId() != null) {
            conversationMemberMapper.delete(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getTenantId, tenantId)
                            .eq(ChatConversationMember::getConversationId, group.getConversationId())
                            .eq(ChatConversationMember::getUserId, currentUserId)
            );
        }

        updateGroupMemberCount(tenantId, groupId);
    }

    @Override
    public void setMemberRole(Long currentUserId, Long tenantId, Long groupId,
                              Long userId, Integer role) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (!currentUserId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.GROUP_PERMISSION_DENIED, "仅群主可设置角色");
        }

        groupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .set(ChatGroupMember::getRole, role)
        );
    }

    @Override
    public void muteMember(Long currentUserId, Long tenantId, Long groupId, Long userId,
                           boolean isMuted, Integer duration) {
        verifyAdminPermission(tenantId, groupId, currentUserId);

        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (userId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.CANNOT_OPERATE_OWNER);
        }

        LocalDateTime muteExpire = (isMuted && duration != null)
                ? LocalDateTime.now().plusSeconds(duration) : null;

        groupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
                        .set(ChatGroupMember::getIsMuted, isMuted ? 1 : 0)
                        .set(ChatGroupMember::getMuteExpireTime, muteExpire)
        );
    }

    @Override
    public void muteAll(Long currentUserId, Long tenantId, Long groupId, boolean isMuted) {
        verifyAdminPermission(tenantId, groupId, currentUserId);

        groupMapper.update(null,
                new LambdaUpdateWrapper<ChatGroup>()
                        .eq(ChatGroup::getId, groupId)
                        .eq(ChatGroup::getTenantId, tenantId)
                        .set(ChatGroup::getIsMuted, isMuted ? 1 : 0)
        );
    }

    @Override
    public void updateNickname(Long currentUserId, Long tenantId, Long groupId, String nickname) {
        groupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, currentUserId)
                        .set(ChatGroupMember::getNickname, nickname)
        );
    }

    @Override
    @Transactional
    public void transferOwner(Long currentUserId, Long tenantId, Long groupId, Long newOwnerId) {
        ChatGroup group = getGroupOrThrow(tenantId, groupId);
        if (!currentUserId.equals(group.getOwnerId())) {
            throw new BusinessException(ChatErrorCode.GROUP_PERMISSION_DENIED, "仅群主可转让");
        }
        verifyGroupMember(tenantId, groupId, newOwnerId);

        // 旧群主降为普通成员
        groupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, currentUserId)
                        .set(ChatGroupMember::getRole, GroupRole.MEMBER.getCode())
        );
        // 新群主
        groupMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, newOwnerId)
                        .set(ChatGroupMember::getRole, GroupRole.OWNER.getCode())
        );
        // 更新群表
        groupMapper.update(null,
                new LambdaUpdateWrapper<ChatGroup>()
                        .eq(ChatGroup::getId, groupId)
                        .set(ChatGroup::getOwnerId, newOwnerId)
        );
    }

    @Override
    public List<Map<String, Object>> listAnnouncements(Long currentUserId, Long tenantId, Long groupId) {
        verifyGroupMember(tenantId, groupId, currentUserId);

        List<ChatGroupAnnouncement> list = announcementMapper.selectList(
                new LambdaQueryWrapper<ChatGroupAnnouncement>()
                        .eq(ChatGroupAnnouncement::getTenantId, tenantId)
                        .eq(ChatGroupAnnouncement::getGroupId, groupId)
                        .orderByDesc(ChatGroupAnnouncement::getIsPinned)
                        .orderByDesc(ChatGroupAnnouncement::getPublishTime)
        );

        return list.stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("content", a.getContent());
                    m.put("publisherId", a.getPublisherId());
                    m.put("isPinned", a.getIsPinned() != null && a.getIsPinned() == 1);
                    m.put("publishTime", a.getPublishTime());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createAnnouncement(Long currentUserId, Long tenantId, Long groupId,
                                                   String title, String content, Boolean isPinned) {
        verifyAdminPermission(tenantId, groupId, currentUserId);

        ChatGroupAnnouncement announcement = new ChatGroupAnnouncement();
        announcement.setTenantId(tenantId);
        announcement.setGroupId(groupId);
        announcement.setPublisherId(currentUserId);
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setIsPinned(Boolean.TRUE.equals(isPinned) ? 1 : 0);
        announcement.setIsConfirmed(0);
        announcement.setConfirmCount(0);
        announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.insert(announcement);

        // WebSocket 推送群公告通知给所有群成员
        pushGroupEvent(tenantId, groupId, "group_announcement", Map.of(
                "announcementId", announcement.getId(),
                "title", title,
                "content", content != null ? content : "",
                "publisherId", currentUserId,
                "isPinned", Boolean.TRUE.equals(isPinned)
        ));

        return Map.of("id", announcement.getId());
    }

    @Override
    public void updateAnnouncement(Long currentUserId, Long tenantId, Long groupId,
                                   Long announcementId, String title, String content, Boolean isPinned) {
        verifyAdminPermission(tenantId, groupId, currentUserId);

        LambdaUpdateWrapper<ChatGroupAnnouncement> wrapper =
                new LambdaUpdateWrapper<ChatGroupAnnouncement>()
                        .eq(ChatGroupAnnouncement::getId, announcementId)
                        .eq(ChatGroupAnnouncement::getTenantId, tenantId)
                        .eq(ChatGroupAnnouncement::getGroupId, groupId);
        if (title != null) wrapper.set(ChatGroupAnnouncement::getTitle, title);
        if (content != null) wrapper.set(ChatGroupAnnouncement::getContent, content);
        if (isPinned != null) wrapper.set(ChatGroupAnnouncement::getIsPinned, isPinned ? 1 : 0);
        announcementMapper.update(null, wrapper);
    }

    @Override
    public void deleteAnnouncement(Long currentUserId, Long tenantId, Long groupId, Long announcementId) {
        verifyAdminPermission(tenantId, groupId, currentUserId);
        announcementMapper.deleteById(announcementId);
    }

    // ======================== 内部工具方法 ========================

    /** 通过 WebSocket 推送群事件通知给所有群成员 */
    private void pushGroupEvent(Long tenantId, Long groupId, String event, Map<String, Object> data) {
        try {
            List<ChatGroupMember> members = groupMemberMapper.selectList(
                    new LambdaQueryWrapper<ChatGroupMember>()
                            .eq(ChatGroupMember::getTenantId, tenantId)
                            .eq(ChatGroupMember::getGroupId, groupId)
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "group_event");
            payload.put("event", event);
            payload.put("groupId", groupId);
            payload.putAll(data);
            String json = JsonUtils.toJsonString(payload);
            for (ChatGroupMember member : members) {
                sessionManager.sendToUser(String.valueOf(member.getUserId()), json);
            }
        } catch (Exception e) {
            log.warn("群事件推送失败, groupId={}, event={}: {}", groupId, event, e.getMessage());
        }
    }

    private ChatGroup getGroupOrThrow(Long tenantId, Long groupId) {
        ChatGroup group = groupMapper.selectById(groupId);
        if (group == null || !tenantId.equals(group.getTenantId())) {
            throw new BusinessException(ChatErrorCode.GROUP_NOT_FOUND);
        }
        if (group.getStatus() != null && group.getStatus() == GroupStatus.DISSOLVED.getCode()) {
            throw new BusinessException(ChatErrorCode.GROUP_DISSOLVED);
        }
        return group;
    }

    private void verifyGroupMember(Long tenantId, Long groupId, Long userId) {
        Long count = groupMemberMapper.selectCount(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );
        if (count == null || count == 0) {
            throw new BusinessException(ChatErrorCode.NOT_GROUP_MEMBER);
        }
    }

    private void verifyAdminPermission(Long tenantId, Long groupId, Long userId) {
        ChatGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
                        .eq(ChatGroupMember::getUserId, userId)
        );
        if (member == null) {
            throw new BusinessException(ChatErrorCode.NOT_GROUP_MEMBER);
        }
        if (member.getRole() < GroupRole.ADMIN.getCode()) {
            throw new BusinessException(ChatErrorCode.GROUP_PERMISSION_DENIED);
        }
    }

    private void addGroupMember(Long tenantId, Long groupId, Long userId,
                                Integer role, Long inviterId) {
        ChatGroupMember member = new ChatGroupMember();
        member.setTenantId(tenantId);
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        member.setInviterId(inviterId);
        member.setIsMuted(0);
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);
    }

    private void addConversationMember(Long tenantId, Long conversationId, Long userId) {
        ChatConversationMember member = new ChatConversationMember();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setUnreadCount(0);
        member.setIsPinned(0);
        member.setIsMuted(0);
        member.setIsHidden(0);
        member.setJoinTime(LocalDateTime.now());
        conversationMemberMapper.insert(member);
    }

    private void updateGroupMemberCount(Long tenantId, Long groupId) {
        Long count = groupMemberMapper.selectCount(
                new LambdaQueryWrapper<ChatGroupMember>()
                        .eq(ChatGroupMember::getTenantId, tenantId)
                        .eq(ChatGroupMember::getGroupId, groupId)
        );
        groupMapper.update(null,
                new LambdaUpdateWrapper<ChatGroup>()
                        .eq(ChatGroup::getId, groupId)
                        .set(ChatGroup::getMemberCount, count != null ? count.intValue() : 0)
        );
    }
}
