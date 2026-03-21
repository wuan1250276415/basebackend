package com.basebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.chat.dto.request.FriendRequestDTO;
import com.basebackend.chat.dto.request.HandleFriendRequestDTO;
import com.basebackend.chat.dto.response.FriendVO;
import com.basebackend.chat.entity.ChatBlacklist;
import com.basebackend.chat.entity.ChatFriend;
import com.basebackend.chat.entity.ChatFriendGroup;
import com.basebackend.chat.entity.ChatFriendRequest;
import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.chat.enums.FriendRequestStatus;
import com.basebackend.chat.enums.FriendStatus;
import com.basebackend.chat.mapper.ChatBlacklistMapper;
import com.basebackend.chat.mapper.ChatFriendGroupMapper;
import com.basebackend.chat.mapper.ChatFriendMapper;
import com.basebackend.chat.mapper.ChatFriendRequestMapper;
import com.basebackend.chat.service.ChatFriendService;
import com.basebackend.common.dto.PageResult;
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
 * 好友服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatFriendServiceImpl implements ChatFriendService {

    private final ChatFriendMapper friendMapper;
    private final ChatFriendGroupMapper friendGroupMapper;
    private final ChatFriendRequestMapper friendRequestMapper;
    private final ChatBlacklistMapper blacklistMapper;
    private final SessionManager sessionManager;

    @Override
    public List<Map<String, Object>> searchUsers(Long currentUserId, Long tenantId, String keyword) {
        // MVP 阶段仅返回空列表占位，实际应调用 user-api 的 Feign 接口
        return List.of();
    }

    @Override
    @Transactional
    public Map<String, Object> sendFriendRequest(Long currentUserId, Long tenantId,
                                                  FriendRequestDTO request) {
        // 检查是否已是好友
        Long existCount = friendMapper.selectCount(
                new LambdaQueryWrapper<ChatFriend>()
                        .eq(ChatFriend::getTenantId, tenantId)
                        .eq(ChatFriend::getUserId, currentUserId)
                        .eq(ChatFriend::getFriendId, request.getToUserId())
                        .eq(ChatFriend::getStatus, FriendStatus.NORMAL.getCode())
        );
        if (existCount != null && existCount > 0) {
            throw new BusinessException(ChatErrorCode.FRIEND_NOT_FOUND, "已经是好友关系");
        }

        // 检查对方是否拉黑了自己
        Long blockedCount = blacklistMapper.selectCount(
                new LambdaQueryWrapper<ChatBlacklist>()
                        .eq(ChatBlacklist::getTenantId, tenantId)
                        .eq(ChatBlacklist::getUserId, request.getToUserId())
                        .eq(ChatBlacklist::getBlockedId, currentUserId)
        );
        if (blockedCount != null && blockedCount > 0) {
            throw new BusinessException(ChatErrorCode.ALREADY_BLOCKED, "对方已将你拉黑");
        }

        ChatFriendRequest friendRequest = new ChatFriendRequest();
        friendRequest.setTenantId(tenantId);
        friendRequest.setFromUserId(currentUserId);
        friendRequest.setToUserId(request.getToUserId());
        friendRequest.setMessage(request.getMessage());
        friendRequest.setSource(request.getSource() != null ? request.getSource() : 0);
        friendRequest.setStatus(FriendRequestStatus.PENDING.getCode());
        friendRequest.setExpireTime(LocalDateTime.now().plusDays(7));
        friendRequestMapper.insert(friendRequest);

        // WebSocket 推送好友申请通知给目标用户
        pushFriendEvent(tenantId, request.getToUserId(), "friend_request", Map.of(
                "requestId", friendRequest.getId(),
                "fromUserId", currentUserId,
                "message", request.getMessage() != null ? request.getMessage() : ""
        ));

        return Map.of("requestId", friendRequest.getId());
    }

    @Override
    @Transactional
    public void handleFriendRequest(Long currentUserId, Long tenantId, Long requestId,
                                    HandleFriendRequestDTO request) {
        ChatFriendRequest friendRequest = friendRequestMapper.selectById(requestId);
        if (friendRequest == null || !tenantId.equals(friendRequest.getTenantId())
                || !currentUserId.equals(friendRequest.getToUserId())) {
            throw new BusinessException(ChatErrorCode.FRIEND_REQUEST_INVALID);
        }
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING.getCode()) {
            throw new BusinessException(ChatErrorCode.FRIEND_REQUEST_INVALID);
        }

        boolean accepted = "accept".equalsIgnoreCase(request.getAction());
        friendRequestMapper.update(null,
                new LambdaUpdateWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getId, requestId)
                        .set(ChatFriendRequest::getStatus,
                                accepted ? FriendRequestStatus.ACCEPTED.getCode()
                                        : FriendRequestStatus.REJECTED.getCode())
                        .set(ChatFriendRequest::getHandleTime, LocalDateTime.now())
        );

        if (accepted) {
            // 双向建立好友关系
            createFriendRelation(tenantId, friendRequest.getFromUserId(), currentUserId,
                    friendRequest.getSource(), null, request.getGroupId());
            createFriendRelation(tenantId, currentUserId, friendRequest.getFromUserId(),
                    friendRequest.getSource(), request.getRemark(), null);

            // WebSocket 推送好友接受通知给申请发起者
            pushFriendEvent(tenantId, friendRequest.getFromUserId(), "friend_accepted", Map.of(
                    "requestId", requestId,
                    "userId", currentUserId
            ));
        }
    }

    @Override
    public PageResult<Map<String, Object>> listFriendRequests(Long currentUserId, Long tenantId,
                                                               Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        Page<ChatFriendRequest> page = friendRequestMapper.selectPage(
                new Page<>(pn, ps),
                new LambdaQueryWrapper<ChatFriendRequest>()
                        .eq(ChatFriendRequest::getTenantId, tenantId)
                        .eq(ChatFriendRequest::getToUserId, currentUserId)
                        .orderByDesc(ChatFriendRequest::getCreateTime)
        );

        List<Map<String, Object>> records = page.getRecords().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("requestId", r.getId());
                    m.put("fromUserId", r.getFromUserId());
                    m.put("message", r.getMessage());
                    m.put("source", r.getSource());
                    m.put("status", r.getStatus());
                    m.put("createTime", r.getCreateTime());
                    m.put("expireTime", r.getExpireTime());
                    return m;
                })
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<FriendVO> listFriends(Long currentUserId, Long tenantId, Long groupId) {
        LambdaQueryWrapper<ChatFriend> wrapper = new LambdaQueryWrapper<ChatFriend>()
                .eq(ChatFriend::getTenantId, tenantId)
                .eq(ChatFriend::getUserId, currentUserId)
                .eq(ChatFriend::getStatus, FriendStatus.NORMAL.getCode());
        if (groupId != null) {
            wrapper.eq(ChatFriend::getGroupId, groupId);
        }

        List<ChatFriend> friends = friendMapper.selectList(wrapper);
        return friends.stream()
                .map(f -> FriendVO.builder()
                        .userId(f.getFriendId())
                        .remark(f.getRemark())
                        .groupId(f.getGroupId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void updateRemark(Long currentUserId, Long tenantId, Long friendUserId, String remark) {
        int updated = friendMapper.update(null,
                new LambdaUpdateWrapper<ChatFriend>()
                        .eq(ChatFriend::getTenantId, tenantId)
                        .eq(ChatFriend::getUserId, currentUserId)
                        .eq(ChatFriend::getFriendId, friendUserId)
                        .eq(ChatFriend::getStatus, FriendStatus.NORMAL.getCode())
                        .set(ChatFriend::getRemark, remark)
        );
        if (updated == 0) {
            throw new BusinessException(ChatErrorCode.FRIEND_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void deleteFriend(Long currentUserId, Long tenantId, Long friendUserId) {
        // 单向删除
        int updated = friendMapper.update(null,
                new LambdaUpdateWrapper<ChatFriend>()
                        .eq(ChatFriend::getTenantId, tenantId)
                        .eq(ChatFriend::getUserId, currentUserId)
                        .eq(ChatFriend::getFriendId, friendUserId)
                        .set(ChatFriend::getStatus, FriendStatus.DELETED.getCode())
        );
        if (updated == 0) {
            throw new BusinessException(ChatErrorCode.FRIEND_NOT_FOUND);
        }
    }

    @Override
    public List<Map<String, Object>> listFriendGroups(Long currentUserId, Long tenantId) {
        List<ChatFriendGroup> groups = friendGroupMapper.selectList(
                new LambdaQueryWrapper<ChatFriendGroup>()
                        .eq(ChatFriendGroup::getTenantId, tenantId)
                        .eq(ChatFriendGroup::getUserId, currentUserId)
                        .orderByAsc(ChatFriendGroup::getSortOrder)
        );
        return groups.stream()
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("groupId", g.getId());
                    m.put("name", g.getName());
                    m.put("sortOrder", g.getSortOrder());
                    m.put("isDefault", g.getIsDefault());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createFriendGroup(Long currentUserId, Long tenantId,
                                                  String name, Integer sortOrder) {
        ChatFriendGroup group = new ChatFriendGroup();
        group.setTenantId(tenantId);
        group.setUserId(currentUserId);
        group.setName(name);
        group.setSortOrder(sortOrder != null ? sortOrder : 0);
        group.setIsDefault(0);
        friendGroupMapper.insert(group);
        return Map.of("groupId", group.getId());
    }

    @Override
    public void updateFriendGroup(Long currentUserId, Long tenantId, Long groupId,
                                  String name, Integer sortOrder) {
        LambdaUpdateWrapper<ChatFriendGroup> wrapper = new LambdaUpdateWrapper<ChatFriendGroup>()
                .eq(ChatFriendGroup::getId, groupId)
                .eq(ChatFriendGroup::getTenantId, tenantId)
                .eq(ChatFriendGroup::getUserId, currentUserId);
        if (name != null) wrapper.set(ChatFriendGroup::getName, name);
        if (sortOrder != null) wrapper.set(ChatFriendGroup::getSortOrder, sortOrder);
        friendGroupMapper.update(null, wrapper);
    }

    @Override
    @Transactional
    public void deleteFriendGroup(Long currentUserId, Long tenantId, Long groupId) {
        // 先把该分组下好友移到默认分组（groupId = null）
        friendMapper.update(null,
                new LambdaUpdateWrapper<ChatFriend>()
                        .eq(ChatFriend::getTenantId, tenantId)
                        .eq(ChatFriend::getUserId, currentUserId)
                        .eq(ChatFriend::getGroupId, groupId)
                        .set(ChatFriend::getGroupId, null)
        );
        friendGroupMapper.deleteById(groupId);
    }

    @Override
    public void moveFriendToGroup(Long currentUserId, Long tenantId, Long friendUserId, Long groupId) {
        friendMapper.update(null,
                new LambdaUpdateWrapper<ChatFriend>()
                        .eq(ChatFriend::getTenantId, tenantId)
                        .eq(ChatFriend::getUserId, currentUserId)
                        .eq(ChatFriend::getFriendId, friendUserId)
                        .set(ChatFriend::getGroupId, groupId)
        );
    }

    @Override
    public void blockUser(Long currentUserId, Long tenantId, Long blockedId, String reason) {
        // 检查是否已拉黑
        Long count = blacklistMapper.selectCount(
                new LambdaQueryWrapper<ChatBlacklist>()
                        .eq(ChatBlacklist::getTenantId, tenantId)
                        .eq(ChatBlacklist::getUserId, currentUserId)
                        .eq(ChatBlacklist::getBlockedId, blockedId)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ChatErrorCode.ALREADY_BLOCKED);
        }

        ChatBlacklist blacklist = new ChatBlacklist();
        blacklist.setTenantId(tenantId);
        blacklist.setUserId(currentUserId);
        blacklist.setBlockedId(blockedId);
        blacklist.setReason(reason);
        blacklistMapper.insert(blacklist);
    }

    @Override
    public void unblockUser(Long currentUserId, Long tenantId, Long blockedUserId) {
        blacklistMapper.delete(
                new LambdaQueryWrapper<ChatBlacklist>()
                        .eq(ChatBlacklist::getTenantId, tenantId)
                        .eq(ChatBlacklist::getUserId, currentUserId)
                        .eq(ChatBlacklist::getBlockedId, blockedUserId)
        );
    }

    @Override
    public List<Map<String, Object>> listBlacklist(Long currentUserId, Long tenantId) {
        List<ChatBlacklist> list = blacklistMapper.selectList(
                new LambdaQueryWrapper<ChatBlacklist>()
                        .eq(ChatBlacklist::getTenantId, tenantId)
                        .eq(ChatBlacklist::getUserId, currentUserId)
        );
        return list.stream()
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", b.getBlockedId());
                    m.put("reason", b.getReason());
                    m.put("blockedTime", b.getCreateTime());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ======================== 内部工具方法 ========================

    /** 通过 WebSocket 推送好友事件通知 */
    private void pushFriendEvent(Long tenantId, Long targetUserId, String event, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "friend_event");
            payload.put("event", event);
            payload.putAll(data);
            sessionManager.sendToUser(String.valueOf(tenantId), String.valueOf(targetUserId),
                    JsonUtils.toJsonString(payload));
        } catch (Exception e) {
            log.warn("好友事件推送失败, targetUserId={}, event={}: {}", targetUserId, event, e.getMessage());
        }
    }

    private void createFriendRelation(Long tenantId, Long userId, Long friendId,
                                      Integer source, String remark, Long groupId) {
        ChatFriend friend = new ChatFriend();
        friend.setTenantId(tenantId);
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setSource(source != null ? source : 0);
        friend.setRemark(remark);
        friend.setGroupId(groupId);
        friend.setStatus(FriendStatus.NORMAL.getCode());
        friendMapper.insert(friend);
    }
}
