package com.basebackend.chat.service;

import com.basebackend.chat.dto.request.SendMessageRequest;
import com.basebackend.chat.dto.request.ForwardMessageRequest;
import com.basebackend.chat.dto.response.MessageVO;

import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 */
public interface ChatMessageService {

    /**
     * 发送消息（REST 通道）
     *
     * @param currentUserId 当前用户ID
     * @param tenantId      租户ID
     * @param request       消息请求
     * @return 消息ID、会话ID、发送时间、状态
     */
    Map<String, Object> sendMessage(Long currentUserId, Long tenantId, SendMessageRequest request);

    /**
     * 获取历史消息
     *
     * @param currentUserId  当前用户ID
     * @param tenantId       租户ID
     * @param conversationId 会话ID
     * @param beforeId       从此消息ID向前加载（不含）
     * @param afterId        从此消息ID向后加载（不含）
     * @param limit          条数
     * @return 消息列表和是否还有更多
     */
    Map<String, Object> getMessages(Long currentUserId, Long tenantId, Long conversationId,
                                    Long beforeId, Long afterId, Integer limit);

    /**
     * 撤回消息
     *
     * @param currentUserId 当前用户ID
     * @param tenantId      租户ID
     * @param messageId     消息ID
     * @return 撤回结果
     */
    Map<String, Object> revokeMessage(Long currentUserId, Long tenantId, Long messageId);

    /**
     * 转发消息
     *
     * @param currentUserId 当前用户ID
     * @param tenantId      租户ID
     * @param request       转发请求
     * @return 转发结果
     */
    Map<String, Object> forwardMessage(Long currentUserId, Long tenantId, ForwardMessageRequest request);

    /**
     * 获取消息已读状态详情（群聊）
     *
     * @param currentUserId 当前用户ID
     * @param tenantId      租户ID
     * @param messageId     消息ID
     * @return 已读详情
     */
    Map<String, Object> getReadStatus(Long currentUserId, Long tenantId, Long messageId);
}
