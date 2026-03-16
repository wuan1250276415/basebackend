package com.basebackend.ticket.notification;

import com.basebackend.ticket.event.TicketNotificationMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketNotificationDispatcherTest {

    @InjectMocks
    private TicketNotificationDispatcher dispatcher;

    @Mock
    private TicketNotificationTemplate template;

    private TicketNotificationMessage buildMessage(Long targetUserId, Long operatorId) {
        return TicketNotificationMessage.builder()
                .notificationType("TICKET_CREATED")
                .ticketId(1L)
                .ticketNo("TK-001")
                .title("测试工单")
                .content("测试内容")
                .targetUserId(targetUserId)
                .targetUserName("目标用户")
                .operatorId(operatorId)
                .operatorName("操作人")
                .eventTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("dispatchCreated - 有目标用户时应发送通知")
    void shouldDispatchCreatedWhenTargetExists() {
        TicketNotificationMessage msg = buildMessage(100L, 1L);
        when(template.buildCreatedTitle(msg)).thenReturn("新工单通知");
        when(template.buildCreatedContent(msg)).thenReturn("内容");

        dispatcher.dispatchCreated(msg);

        verify(template).buildCreatedTitle(msg);
        verify(template).buildCreatedContent(msg);
    }

    @Test
    @DisplayName("dispatchCreated - 无目标用户时应跳过")
    void shouldSkipCreatedWhenNoTarget() {
        TicketNotificationMessage msg = buildMessage(null, 1L);

        dispatcher.dispatchCreated(msg);

        verify(template, never()).buildCreatedTitle(any());
    }

    @Test
    @DisplayName("dispatchAssigned - 应通知新处理人")
    void shouldDispatchAssigned() {
        TicketNotificationMessage msg = buildMessage(200L, 1L);
        when(template.buildAssignedTitle(msg)).thenReturn("分配通知");
        when(template.buildAssignedContent(msg)).thenReturn("内容");

        dispatcher.dispatchAssigned(msg);

        verify(template).buildAssignedTitle(msg);
    }

    @Test
    @DisplayName("dispatchStatusChanged - 应通知相关方")
    void shouldDispatchStatusChanged() {
        TicketNotificationMessage msg = buildMessage(100L, 200L);
        when(template.buildStatusChangedTitle(msg)).thenReturn("状态变更");
        when(template.buildStatusChangedContent(msg)).thenReturn("内容");

        dispatcher.dispatchStatusChanged(msg);

        verify(template).buildStatusChangedTitle(msg);
    }
}
