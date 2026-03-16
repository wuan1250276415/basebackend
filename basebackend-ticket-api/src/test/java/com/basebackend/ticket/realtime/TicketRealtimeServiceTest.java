package com.basebackend.ticket.realtime;

import com.basebackend.websocket.channel.ChannelManager;
import com.basebackend.websocket.session.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketRealtimeServiceTest {

    @InjectMocks
    private TicketRealtimeService realtimeService;

    @Mock
    private SessionManager sessionManager;

    @Mock
    private ChannelManager channelManager;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("notifyTicketUpdate - 有订阅者时应推送")
    void shouldNotifyWhenMembersExist() {
        when(channelManager.getMembers("ticket:1")).thenReturn(Set.of("user1", "user2"));
        when(sessionManager.sendToUser(anyString(), anyString())).thenReturn(1);

        realtimeService.notifyTicketUpdate(1L, "STATUS_CHANGED", Map.of("status", "RESOLVED"));

        verify(sessionManager, times(2)).sendToUser(anyString(), anyString());
    }

    @Test
    @DisplayName("notifyTicketUpdate - 无订阅者时应跳过")
    void shouldSkipWhenNoMembers() {
        when(channelManager.getMembers("ticket:1")).thenReturn(Collections.emptySet());

        realtimeService.notifyTicketUpdate(1L, "STATUS_CHANGED", Map.of());

        verify(sessionManager, never()).sendToUser(anyString(), anyString());
    }

    @Test
    @DisplayName("subscribeTicket - 应加入频道")
    void shouldSubscribeTicket() {
        realtimeService.subscribeTicket(1L, "user1");

        verify(channelManager).join("ticket:1", "user1");
    }

    @Test
    @DisplayName("unsubscribeTicket - 应离开频道")
    void shouldUnsubscribeTicket() {
        realtimeService.unsubscribeTicket(1L, "user1");

        verify(channelManager).leave("ticket:1", "user1");
    }
}
