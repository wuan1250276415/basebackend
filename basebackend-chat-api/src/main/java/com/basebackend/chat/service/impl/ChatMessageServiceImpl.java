package com.basebackend.chat.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.chat.dto.request.ForwardMessageRequest;
import com.basebackend.chat.dto.request.SendMessageRequest;
import com.basebackend.chat.entity.ChatBlacklist;
import com.basebackend.chat.entity.ChatConversation;
import com.basebackend.chat.entity.ChatConversationMember;
import com.basebackend.chat.entity.ChatGroup;
import com.basebackend.chat.entity.ChatGroupMember;
import com.basebackend.chat.entity.ChatMessage;
import com.basebackend.chat.entity.ChatMessageForward;
import com.basebackend.chat.entity.ChatMessageRead;
import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.chat.enums.ConversationType;
import com.basebackend.chat.enums.MessageStatus;
import com.basebackend.chat.mapper.ChatBlacklistMapper;
import com.basebackend.chat.mapper.ChatConversationMapper;
import com.basebackend.chat.mapper.ChatConversationMemberMapper;
import com.basebackend.chat.mapper.ChatGroupMapper;
import com.basebackend.chat.mapper.ChatGroupMemberMapper;
import com.basebackend.chat.mapper.ChatMessageForwardMapper;
import com.basebackend.chat.mapper.ChatMessageMapper;
import com.basebackend.chat.mapper.ChatMessageReadMapper;
import com.basebackend.chat.service.ChatMessageSearchService;
import com.basebackend.chat.service.ChatMessageService;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.websocket.session.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper messageMapper;
    private final ChatMessageReadMapper messageReadMapper;
    private final ChatMessageForwardMapper messageForwardMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper conversationMemberMapper;
    private final ChatBlacklistMapper blacklistMapper;
    private final ChatGroupMapper groupMapper;
    private final ChatGroupMemberMapper groupMemberMapper;
    private final StringRedisTemplate redisTemplate;
    private final SessionManager sessionManager;
    /** 搜索服务（可选依赖，仅当 search.enabled=true 时可用） */
    private final ObjectProvider<ChatMessageSearchService> searchServiceProvider;

    /** 撤回时限: 2分钟 */
    private static final long REVOKE_TIMEOUT_SECONDS = 120;

    @Override
    @Transactional
    public Map<String, Object> sendMessage(Long currentUserId, Long tenantId, SendMessageRequest request) {
        // 客户端消息去重
        if (StrUtil.isNotBlank(request.getClientMsgId())) {
            String dedupKey = "chat:msg:dedup:" + request.getClientMsgId();
            Boolean absent = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", Duration.ofMinutes(5));
            if (Boolean.FALSE.equals(absent)) {
                throw new BusinessException(ChatErrorCode.MESSAGE_SEND_FAILED, "消息重复发送");
            }
        }

        // 验证会话存在且当前用户是成员
        ChatConversation conversation = conversationMapper.selectById(request.getConversationId());
        if (conversation == null || !tenantId.equals(conversation.getTenantId())) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND);
        }
        verifyConversationMember(tenantId, request.getConversationId(), currentUserId);

        // 私聊场景：黑名单校验 — 对方拉黑了自己则不允许发消息
        if (conversation.getType() == ConversationType.PRIVATE.getCode()) {
            Long targetUserId = conversation.getTargetId();
            Long blockedCount = blacklistMapper.selectCount(
                    new LambdaQueryWrapper<ChatBlacklist>()
                            .eq(ChatBlacklist::getTenantId, tenantId)
                            .eq(ChatBlacklist::getUserId, targetUserId)
                            .eq(ChatBlacklist::getBlockedId, currentUserId)
            );
            if (blockedCount != null && blockedCount > 0) {
                throw new BusinessException(ChatErrorCode.ALREADY_BLOCKED, "消息发送失败，你已被对方拉黑");
            }
        }

        // 群聊场景：禁言校验 — 群全员禁言或成员个人禁言时阻止发言
        if (conversation.getType() == ConversationType.GROUP.getCode()) {
            ChatGroup group = groupMapper.selectOne(
                    new LambdaQueryWrapper<ChatGroup>()
                            .eq(ChatGroup::getConversationId, conversation.getId())
                            .eq(ChatGroup::getTenantId, tenantId)
            );
            if (group != null) {
                ChatGroupMember member = groupMemberMapper.selectOne(
                        new LambdaQueryWrapper<ChatGroupMember>()
                                .eq(ChatGroupMember::getTenantId, tenantId)
                                .eq(ChatGroupMember::getGroupId, group.getId())
                                .eq(ChatGroupMember::getUserId, currentUserId)
                );
                if (member != null) {
                    // 群全员禁言（管理员和群主除外）
                    boolean isGroupMuted = group.getIsMuted() != null && group.getIsMuted() == 1;
                    boolean isAdmin = member.getRole() != null && member.getRole() >= 1;
                    if (isGroupMuted && !isAdmin) {
                        throw new BusinessException(ChatErrorCode.USER_MUTED, "群全员禁言中");
                    }
                    // 成员个人禁言
                    if (member.getIsMuted() != null && member.getIsMuted() == 1) {
                        // 检查禁言是否过期
                        if (member.getMuteExpireTime() == null
                                || member.getMuteExpireTime().isAfter(LocalDateTime.now())) {
                            throw new BusinessException(ChatErrorCode.USER_MUTED);
                        }
                    }
                }
            }
        }

        // 构建消息实体
        LocalDateTime now = LocalDateTime.now();
        ChatMessage message = new ChatMessage();
        message.setTenantId(tenantId);
        message.setConversationId(request.getConversationId());
        message.setSenderId(currentUserId);
        message.setSenderName("");  // 由上层填充，或从用户缓存获取
        message.setType(request.getType());
        message.setContent(request.getContent());
        message.setClientMsgId(request.getClientMsgId());
        message.setQuoteMessageId(request.getQuoteMessageId());
        message.setSendTime(now);
        message.setStatus(MessageStatus.SENT.getCode());
        if (request.getAtUserIds() != null) {
            message.setAtUserIds(JsonUtils.toJsonString(request.getAtUserIds()));
        }
        if (request.getExtra() != null) {
            message.setExtra(request.getExtra());
        }

        messageMapper.insert(message);

        // 更新会话最后消息快照
        String preview = buildPreview(request.getType(), request.getContent());
        updateConversationLastMessage(conversation, message.getId(), now, preview, currentUserId);

        // 更新其他成员未读数
        incrementUnreadCount(tenantId, request.getConversationId(), currentUserId);

        // 通过 WebSocket 推送给会话内在线成员
        pushToConversationMembers(tenantId, request.getConversationId(), currentUserId, message);

        // 异步索引到搜索引擎（仅当搜索服务可用时）
        ChatMessageSearchService searchService = searchServiceProvider.getIfAvailable();
        if (searchService != null) {
            searchService.indexMessage(message);
        }

        return Map.of(
                "messageId", message.getId(),
                "conversationId", message.getConversationId(),
                "sendTime", now,
                "status", message.getStatus()
        );
    }

    @Override
    public Map<String, Object> getMessages(Long currentUserId, Long tenantId, Long conversationId,
                                           Long beforeId, Long afterId, Integer limit) {
        verifyConversationMember(tenantId, conversationId, currentUserId);

        int queryLimit = (limit == null || limit <= 0) ? 30 : Math.min(limit, 100);

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getTenantId, tenantId)
                .eq(ChatMessage::getConversationId, conversationId);

        if (beforeId != null) {
            wrapper.lt(ChatMessage::getId, beforeId);
        }
        if (afterId != null) {
            wrapper.gt(ChatMessage::getId, afterId);
        }
        wrapper.orderByDesc(ChatMessage::getId)
               .last("LIMIT " + (queryLimit + 1));

        List<ChatMessage> messages = messageMapper.selectList(wrapper);
        boolean hasMore = messages.size() > queryLimit;
        if (hasMore) {
            messages = messages.subList(0, queryLimit);
        }

        List<Map<String, Object>> messageList = messages.stream()
                .map(this::toMessageMap)
                .collect(Collectors.toList());

        return Map.of("messages", messageList, "hasMore", hasMore);
    }

    @Override
    @Transactional
    public Map<String, Object> revokeMessage(Long currentUserId, Long tenantId, Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null || !tenantId.equals(message.getTenantId())) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND, "消息不存在");
        }
        if (!currentUserId.equals(message.getSenderId())) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NO_PERMISSION, "只能撤回自己发送的消息");
        }

        // 检查撤回时限
        Duration elapsed = Duration.between(message.getSendTime(), LocalDateTime.now());
        if (elapsed.getSeconds() > REVOKE_TIMEOUT_SECONDS) {
            throw new BusinessException(ChatErrorCode.MESSAGE_REVOKE_TIMEOUT);
        }

        LocalDateTime revokeTime = LocalDateTime.now();
        LambdaUpdateWrapper<ChatMessage> updateWrapper = new LambdaUpdateWrapper<ChatMessage>()
                .eq(ChatMessage::getId, messageId)
                .set(ChatMessage::getStatus, MessageStatus.REVOKED.getCode())
                .set(ChatMessage::getRevokeTime, revokeTime);
        messageMapper.update(null, updateWrapper);

        // 推送撤回通知给会话成员
        pushRevokeNotification(tenantId, message.getConversationId(), messageId);

        return Map.of("messageId", messageId, "revokeTime", revokeTime);
    }

    @Override
    @Transactional
    public Map<String, Object> forwardMessage(Long currentUserId, Long tenantId,
                                              ForwardMessageRequest request) {
        List<ChatMessage> originalMessages = messageMapper.selectBatchIds(request.getMessageIds());
        if (originalMessages.isEmpty()) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND, "原始消息不存在");
        }

        int forwardedCount = 0;
        for (Long targetConversationId : request.getTargetConversationIds()) {
            verifyConversationMember(tenantId, targetConversationId, currentUserId);

            if ("merge".equals(request.getForwardType())) {
                // 合并转发 — 保存原始消息快照到 chat_message_forward，然后发一条合并消息
                String forwardId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                int seq = 0;
                for (ChatMessage orig : originalMessages) {
                    ChatMessageForward forward = new ChatMessageForward();
                    forward.setTenantId(tenantId);
                    forward.setForwardId(forwardId);
                    forward.setOriginalMsgId(orig.getId());
                    forward.setOriginalConversationId(orig.getConversationId());
                    forward.setOriginalSenderId(orig.getSenderId());
                    forward.setOriginalSenderName(orig.getSenderName());
                    forward.setOriginalContent(orig.getContent());
                    forward.setOriginalContentType(orig.getType());
                    forward.setOriginalSendTime(orig.getSendTime());
                    forward.setSeqNo(seq++);
                    messageForwardMapper.insert(forward);
                }

                // 往目标会话发送一条类型为 MERGE_FORWARD(11) 的消息
                SendMessageRequest mergeReq = new SendMessageRequest();
                mergeReq.setConversationId(targetConversationId);
                mergeReq.setType(11);
                mergeReq.setContent(request.getTitle() != null ? request.getTitle() : "聊天记录");
                mergeReq.setClientMsgId(UUID.randomUUID().toString());
                sendMessage(currentUserId, tenantId, mergeReq);
                forwardedCount++;
            } else {
                // 逐条转发
                for (ChatMessage orig : originalMessages) {
                    SendMessageRequest singleReq = new SendMessageRequest();
                    singleReq.setConversationId(targetConversationId);
                    singleReq.setType(orig.getType());
                    singleReq.setContent(orig.getContent());
                    singleReq.setExtra(orig.getExtra());
                    singleReq.setClientMsgId(UUID.randomUUID().toString());
                    sendMessage(currentUserId, tenantId, singleReq);
                }
                forwardedCount++;
            }
        }

        return Map.of(
                "forwardedCount", request.getMessageIds().size(),
                "targetCount", forwardedCount
        );
    }

    @Override
    public Map<String, Object> getReadStatus(Long currentUserId, Long tenantId, Long messageId) {
        ChatMessage message = messageMapper.selectById(messageId);
        if (message == null || !tenantId.equals(message.getTenantId())) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NOT_FOUND, "消息不存在");
        }

        // 查询已读记录
        List<ChatMessageRead> reads = messageReadMapper.selectList(
                new LambdaQueryWrapper<ChatMessageRead>()
                        .eq(ChatMessageRead::getTenantId, tenantId)
                        .eq(ChatMessageRead::getMessageId, messageId)
        );

        // 查询会话总成员数
        ChatConversation conversation = conversationMapper.selectById(message.getConversationId());
        int totalMembers = conversation != null ? conversation.getMemberCount() : 0;
        int readCount = reads.size();

        List<Map<String, Object>> readUsers = reads.stream()
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", r.getUserId());
                    m.put("readTime", r.getReadTime());
                    return m;
                })
                .collect(Collectors.toList());

        return Map.of(
                "messageId", messageId,
                "totalMembers", totalMembers,
                "readCount", readCount,
                "unreadCount", Math.max(0, totalMembers - readCount - 1),
                "readUsers", readUsers
        );
    }

    // ======================== 内部工具方法 ========================

    /** 验证用户是否为会话成员 */
    private void verifyConversationMember(Long tenantId, Long conversationId, Long userId) {
        Long count = conversationMemberMapper.selectCount(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId)
        );
        if (count == null || count == 0) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NO_PERMISSION);
        }
    }

    /** 构建消息预览文本 */
    private String buildPreview(Integer type, String content) {
        return switch (type) {
            case 1 -> content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content;
            case 2 -> "[图片]";
            case 3 -> "[文件]";
            case 4 -> "[语音]";
            case 5 -> "[视频]";
            case 6 -> "[位置]";
            case 7 -> "[名片]";
            case 8 -> "[表情]";
            case 9 -> "[系统通知]";
            case 10 -> "撤回了一条消息";
            case 11 -> "[聊天记录]";
            default -> "[消息]";
        };
    }

    /** 更新会话最后消息快照 */
    private void updateConversationLastMessage(ChatConversation conversation,
                                               Long messageId, LocalDateTime sendTime,
                                               String preview, Long senderId) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<ChatConversation>()
                .eq(ChatConversation::getId, conversation.getId())
                .set(ChatConversation::getLastMessageId, messageId)
                .set(ChatConversation::getLastMessageTime, sendTime)
                .set(ChatConversation::getLastMessagePreview, preview)
                .set(ChatConversation::getLastSenderId, senderId);
        conversationMapper.update(null, wrapper);
    }

    /** 递增其他会话成员的未读数 */
    private void incrementUnreadCount(Long tenantId, Long conversationId, Long excludeUserId) {
        List<ChatConversationMember> members = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .ne(ChatConversationMember::getUserId, excludeUserId)
        );
        for (ChatConversationMember member : members) {
            conversationMemberMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getId, member.getId())
                            .setSql("unread_count = unread_count + 1")
                            .set(ChatConversationMember::getIsHidden, 0)
            );
            // 更新 Redis 未读计数
            String key = "chat:unread:" + tenantId + ":" + member.getUserId() + ":" + conversationId;
            redisTemplate.opsForValue().increment(key);
        }
    }

    /** 通过 WebSocket 推送消息给会话成员 */
    private void pushToConversationMembers(Long tenantId, Long conversationId,
                                           Long senderId, ChatMessage message) {
        List<ChatConversationMember> members = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .ne(ChatConversationMember::getUserId, senderId)
        );
        String payload = JsonUtils.toJsonString(toMessageMap(message));
        for (ChatConversationMember member : members) {
            sessionManager.sendToUser(String.valueOf(member.getUserId()), payload);
        }
    }

    /** 推送撤回通知 */
    private void pushRevokeNotification(Long tenantId, Long conversationId, Long messageId) {
        Map<String, Object> notification = Map.of(
                "type", "revoke",
                "conversationId", conversationId,
                "messageId", messageId
        );
        String payload = JsonUtils.toJsonString(notification);

        List<ChatConversationMember> members = conversationMemberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
        );
        for (ChatConversationMember member : members) {
            sessionManager.sendToUser(String.valueOf(member.getUserId()), payload);
        }
    }

    /** 实体转 Map */
    private Map<String, Object> toMessageMap(ChatMessage msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("messageId", msg.getId());
        m.put("conversationId", msg.getConversationId());
        m.put("senderId", msg.getSenderId());
        m.put("senderName", msg.getSenderName());
        m.put("senderAvatar", msg.getSenderAvatar());
        m.put("type", msg.getType());
        m.put("content", msg.getContent());
        m.put("extra", msg.getExtra());
        m.put("quoteMessageId", msg.getQuoteMessageId());
        m.put("atUserIds", msg.getAtUserIds());
        m.put("clientMsgId", msg.getClientMsgId());
        m.put("sendTime", msg.getSendTime());
        m.put("status", msg.getStatus());
        return m;
    }
}
