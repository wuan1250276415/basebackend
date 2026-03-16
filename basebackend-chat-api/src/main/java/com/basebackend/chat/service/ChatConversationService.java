package com.basebackend.chat.service;

import com.basebackend.chat.dto.request.CreateConversationRequest;
import com.basebackend.chat.dto.response.ConversationVO;
import com.basebackend.common.dto.PageResult;

import java.util.Map;

/**
 * 会话服务接口
 */
public interface ChatConversationService {

    /**
     * 获取当前用户的会话列表（分页，按最后消息时间倒序）
     */
    PageResult<ConversationVO> listConversations(Long currentUserId, Long tenantId,
                                                  Integer pageNum, Integer pageSize);

    /**
     * 创建/打开会话（幂等：私聊已存在则返回已有会话）
     */
    Map<String, Object> createOrOpen(Long currentUserId, Long tenantId,
                                     CreateConversationRequest request);

    /**
     * 删除（隐藏）会话
     */
    void deleteConversation(Long currentUserId, Long tenantId, Long conversationId);

    /**
     * 置顶/取消置顶
     */
    void pinConversation(Long currentUserId, Long tenantId, Long conversationId, boolean isPinned);

    /**
     * 免打扰设置
     */
    void muteConversation(Long currentUserId, Long tenantId, Long conversationId, boolean isMuted);

    /**
     * 保存草稿
     */
    void saveDraft(Long currentUserId, Long tenantId, Long conversationId, String draft);

    /**
     * 标记已读 / 清空未读
     *
     * @return 清除的未读数
     */
    int markAsRead(Long currentUserId, Long tenantId, Long conversationId, Long lastReadMessageId);
}
