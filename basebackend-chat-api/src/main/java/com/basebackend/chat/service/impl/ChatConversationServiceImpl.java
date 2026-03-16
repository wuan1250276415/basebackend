package com.basebackend.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.basebackend.chat.dto.request.CreateConversationRequest;
import com.basebackend.chat.dto.response.ConversationVO;
import com.basebackend.chat.entity.ChatConversation;
import com.basebackend.chat.entity.ChatConversationMember;
import com.basebackend.chat.enums.ChatErrorCode;
import com.basebackend.chat.enums.ConversationType;
import com.basebackend.chat.mapper.ChatConversationMapper;
import com.basebackend.chat.mapper.ChatConversationMemberMapper;
import com.basebackend.chat.service.ChatConversationService;
import com.basebackend.common.dto.PageResult;
import com.basebackend.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatConversationServiceImpl implements ChatConversationService {

    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper conversationMemberMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<ConversationVO> listConversations(Long currentUserId, Long tenantId,
                                                         Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        // 查询当前用户加入的、未隐藏的会话成员记录
        Page<ChatConversationMember> memberPage = conversationMemberMapper.selectPage(
                new Page<>(pn, ps),
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
                        .eq(ChatConversationMember::getIsHidden, 0)
                        .orderByDesc(ChatConversationMember::getIsPinned)
        );

        if (memberPage.getRecords().isEmpty()) {
            return PageResult.empty();
        }

        // 批量查询会话信息
        List<Long> conversationIds = memberPage.getRecords().stream()
                .map(ChatConversationMember::getConversationId)
                .collect(Collectors.toList());
        List<ChatConversation> conversations = conversationMapper.selectBatchIds(conversationIds);
        Map<Long, ChatConversation> convMap = conversations.stream()
                .collect(Collectors.toMap(ChatConversation::getId, c -> c));

        List<ConversationVO> voList = memberPage.getRecords().stream()
                .map(member -> {
                    ChatConversation conv = convMap.get(member.getConversationId());
                    if (conv == null) return null;

                    var lastMsg = conv.getLastMessageId() != null ?
                            ConversationVO.LastMessageVO.builder()
                                    .messageId(conv.getLastMessageId())
                                    .content(conv.getLastMessagePreview())
                                    .sendTime(conv.getLastMessageTime())
                                    .build() : null;

                    return ConversationVO.builder()
                            .conversationId(conv.getId())
                            .type(conv.getType())
                            .targetId(conv.getTargetId())
                            .lastMessage(lastMsg)
                            .unreadCount(member.getUnreadCount())
                            .isPinned(member.getIsPinned() != null && member.getIsPinned() == 1)
                            .isMuted(member.getIsMuted() != null && member.getIsMuted() == 1)
                            .memberCount(conv.getMemberCount())
                            .draft(member.getDraft())
                            .updateTime(conv.getLastMessageTime() != null ?
                                    conv.getLastMessageTime() : conv.getCreateTime())
                            .build();
                })
                .filter(v -> v != null)
                .collect(Collectors.toList());

        return PageResult.of(voList, memberPage.getTotal(),
                memberPage.getCurrent(), memberPage.getSize());
    }

    @Override
    @Transactional
    public Map<String, Object> createOrOpen(Long currentUserId, Long tenantId,
                                            CreateConversationRequest request) {
        boolean isPm = request.getType() == ConversationType.PRIVATE.getCode();

        if (isPm) {
            // 私聊幂等 — 查找是否已存在
            ChatConversationMember existing = conversationMemberMapper.selectOne(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getTenantId, tenantId)
                            .eq(ChatConversationMember::getUserId, currentUserId)
                            .inSql(ChatConversationMember::getConversationId,
                                    "SELECT id FROM chat_conversation WHERE tenant_id = " + tenantId
                                            + " AND type = 1 AND target_id = " + request.getTargetId()
                                            + " AND deleted = 0")
                            .last("LIMIT 1")
            );
            if (existing != null) {
                // 取消隐藏
                if (existing.getIsHidden() != null && existing.getIsHidden() == 1) {
                    conversationMemberMapper.update(null,
                            new LambdaUpdateWrapper<ChatConversationMember>()
                                    .eq(ChatConversationMember::getId, existing.getId())
                                    .set(ChatConversationMember::getIsHidden, 0)
                    );
                }
                return Map.of(
                        "conversationId", existing.getConversationId(),
                        "type", ConversationType.PRIVATE.getCode(),
                        "targetId", request.getTargetId(),
                        "created", false
                );
            }
        }

        // 创建新会话
        ChatConversation conversation = new ChatConversation();
        conversation.setTenantId(tenantId);
        conversation.setType(request.getType());
        conversation.setTargetId(request.getTargetId());
        conversation.setMemberCount(isPm ? 2 : 1);
        conversationMapper.insert(conversation);

        // 添加当前用户为成员
        addConversationMember(tenantId, conversation.getId(), currentUserId);

        // 私聊添加对方为成员
        if (isPm) {
            addConversationMember(tenantId, conversation.getId(), request.getTargetId());
        }

        return Map.of(
                "conversationId", conversation.getId(),
                "type", request.getType(),
                "targetId", request.getTargetId(),
                "created", true
        );
    }

    @Override
    @Transactional
    public void deleteConversation(Long currentUserId, Long tenantId, Long conversationId) {
        conversationMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
                        .set(ChatConversationMember::getIsHidden, 1)
        );
    }

    @Override
    public void pinConversation(Long currentUserId, Long tenantId, Long conversationId, boolean isPinned) {
        conversationMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
                        .set(ChatConversationMember::getIsPinned, isPinned ? 1 : 0)
        );
    }

    @Override
    public void muteConversation(Long currentUserId, Long tenantId, Long conversationId, boolean isMuted) {
        conversationMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
                        .set(ChatConversationMember::getIsMuted, isMuted ? 1 : 0)
        );
    }

    @Override
    public void saveDraft(Long currentUserId, Long tenantId, Long conversationId, String draft) {
        conversationMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
                        .set(ChatConversationMember::getDraft, draft)
        );
    }

    @Override
    @Transactional
    public int markAsRead(Long currentUserId, Long tenantId, Long conversationId, Long lastReadMessageId) {
        ChatConversationMember member = conversationMemberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getTenantId, tenantId)
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, currentUserId)
        );
        if (member == null) {
            throw new BusinessException(ChatErrorCode.CONVERSATION_NO_PERMISSION);
        }

        int clearedCount = member.getUnreadCount() != null ? member.getUnreadCount() : 0;

        conversationMemberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getId, member.getId())
                        .set(ChatConversationMember::getUnreadCount, 0)
                        .set(ChatConversationMember::getLastReadMessageId, lastReadMessageId)
                        .set(ChatConversationMember::getLastReadTime, LocalDateTime.now())
        );

        // 清除 Redis 未读计数
        String key = "chat:unread:" + tenantId + ":" + currentUserId + ":" + conversationId;
        redisTemplate.delete(key);

        return clearedCount;
    }

    // ======================== 内部工具方法 ========================

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
}
