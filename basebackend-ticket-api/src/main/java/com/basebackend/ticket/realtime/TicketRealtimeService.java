package com.basebackend.ticket.realtime;

import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.message.WsMessage;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

/**
 * 工单实时推送服务
 * <p>通过 WebSocket 推送工单变更事件给关注的用户</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "basebackend.websocket.enabled", havingValue = "true", matchIfMissing = false)
public class TicketRealtimeService {

    private static final String CHANNEL_PREFIX = "ticket:";

    private final SessionManager sessionManager;
    private final ChannelManager channelManager;
    private final ObjectMapper objectMapper;

    /**
     * 推送工单更新事件给所有关注该工单的用户
     */
    public void notifyTicketUpdate(Long ticketId, String eventType, Object payload) {
        String channelId = CHANNEL_PREFIX + ticketId;
        Set<String> members = channelManager.getMembers(channelId);
        if (members.isEmpty()) {
            log.debug("工单频道无订阅者，跳过推送: ticketId={}", ticketId);
            return;
        }

        WsMessage message = WsMessage.event(eventType, Map.of(
                "ticketId", ticketId,
                "eventType", eventType,
                "payload", payload
        ));

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            for (String userId : members) {
                sessionManager.sendToUser(userId, messageJson);
            }
            log.debug("工单实时推送: ticketId={}, eventType={}, members={}", ticketId, eventType, members.size());
        } catch (Exception e) {
            log.error("工单实时推送失败: ticketId={}", ticketId, e);
        }
    }

    /**
     * 推送事件给指定用户
     */
    public void notifyUserTicketEvent(Long userId, String eventType, Object payload) {
        WsMessage message = WsMessage.event(eventType, Map.of("payload", payload));
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            sessionManager.sendToUser(String.valueOf(userId), messageJson);
        } catch (Exception e) {
            log.error("推送用户工单事件失败: userId={}", userId, e);
        }
    }

    /**
     * 用户订阅工单频道
     */
    public void subscribeTicket(Long ticketId, String userId) {
        String channelId = CHANNEL_PREFIX + ticketId;
        channelManager.join(channelId, userId);
        log.debug("用户订阅工单频道: ticketId={}, userId={}", ticketId, userId);
    }

    /**
     * 用户取消订阅工单频道
     */
    public void unsubscribeTicket(Long ticketId, String userId) {
        String channelId = CHANNEL_PREFIX + ticketId;
        channelManager.leave(channelId, userId);
        log.debug("用户取消订阅工单频道: ticketId={}, userId={}", ticketId, userId);
    }
}
