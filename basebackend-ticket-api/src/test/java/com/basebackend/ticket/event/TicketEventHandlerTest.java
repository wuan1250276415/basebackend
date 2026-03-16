package com.basebackend.ticket.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketEventHandler 工单事件处理器测试")
class TicketEventHandlerTest {

    @Mock private MessageProducer messageProducer;

    @InjectMocks
    private TicketEventHandler eventHandler;

    @SuppressWarnings("unchecked")
    private void stubSendAsync() {
        given(messageProducer.sendAsync(any(Message.class)))
                .willReturn(CompletableFuture.completedFuture("msg-id-001"));
    }

    @Test
    @DisplayName("onTicketCreated - 有处理人时应发送通知")
    @SuppressWarnings("unchecked")
    void shouldSendNotificationWhenAssigneePresent() {
        stubSendAsync();

        TicketCreatedEvent event = new TicketCreatedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                100L, "张三", 200L, "李四", "测试工单", 3
        );

        eventHandler.onTicketCreated(event);

        ArgumentCaptor<Message<TicketNotificationMessage>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).sendAsync(captor.capture());

        Message<TicketNotificationMessage> msg = captor.getValue();
        assertThat(msg.getTopic()).isEqualTo("ticket-notification-topic");
        assertThat(msg.getTags()).isEqualTo("CREATED");
        assertThat(msg.getPayload().getNotificationType()).isEqualTo("TICKET_CREATED");
        assertThat(msg.getPayload().getTicketNo()).isEqualTo("TK-20260301-0001");
        assertThat(msg.getPayload().getTargetUserId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("onTicketCreated - 无处理人时不应发送通知")
    void shouldNotSendWhenNoAssignee() {
        TicketCreatedEvent event = new TicketCreatedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                100L, "张三", null, null, "测试工单", 3
        );

        eventHandler.onTicketCreated(event);

        verify(messageProducer, org.mockito.Mockito.never()).sendAsync(any());
    }

    @Test
    @DisplayName("onTicketStatusChanged - 应发送状态变更通知")
    @SuppressWarnings("unchecked")
    void shouldSendStatusChangedNotification() {
        stubSendAsync();

        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                "OPEN", "IN_PROGRESS", 100L, "张三", "开始处理"
        );

        eventHandler.onTicketStatusChanged(event);

        ArgumentCaptor<Message<TicketNotificationMessage>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).sendAsync(captor.capture());

        Message<TicketNotificationMessage> msg = captor.getValue();
        assertThat(msg.getTags()).isEqualTo("STATUS_CHANGED");
        assertThat(msg.getPayload().getNotificationType()).isEqualTo("STATUS_CHANGED");
        assertThat(msg.getPayload().getContent()).contains("OPEN").contains("IN_PROGRESS");
    }

    @Test
    @DisplayName("onTicketAssigned - 应发送分配通知")
    @SuppressWarnings("unchecked")
    void shouldSendAssignedNotification() {
        stubSendAsync();

        TicketAssignedEvent event = new TicketAssignedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                200L, "李四", 100L, "张三"
        );

        eventHandler.onTicketAssigned(event);

        ArgumentCaptor<Message<TicketNotificationMessage>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).sendAsync(captor.capture());

        Message<TicketNotificationMessage> msg = captor.getValue();
        assertThat(msg.getTags()).isEqualTo("ASSIGNED");
        assertThat(msg.getPayload().getTargetUserId()).isEqualTo(200L);
        assertThat(msg.getPayload().getTargetUserName()).isEqualTo("李四");
    }

    @Test
    @DisplayName("onTicketApproved - 应发送审批通知")
    @SuppressWarnings("unchecked")
    void shouldSendApprovalNotification() {
        stubSendAsync();

        TicketApprovedEvent event = new TicketApprovedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                "APPROVE", 300L, "王五", "同意"
        );

        eventHandler.onTicketApproved(event);

        ArgumentCaptor<Message<TicketNotificationMessage>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).sendAsync(captor.capture());

        Message<TicketNotificationMessage> msg = captor.getValue();
        assertThat(msg.getTags()).isEqualTo("APPROVAL_APPROVE");
        assertThat(msg.getPayload().getNotificationType()).isEqualTo("TICKET_APPROVAL");
        assertThat(msg.getPayload().getContent()).contains("审批APPROVE").contains("同意");
    }

    @Test
    @DisplayName("onTicketApproved - 无意见时内容不含冒号")
    @SuppressWarnings("unchecked")
    void shouldNotAppendColonWhenNoOpinion() {
        stubSendAsync();

        TicketApprovedEvent event = new TicketApprovedEvent(
                "ticket-service", 1L, "TK-20260301-0001",
                "REJECT", 300L, "王五", null
        );

        eventHandler.onTicketApproved(event);

        ArgumentCaptor<Message<TicketNotificationMessage>> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).sendAsync(captor.capture());

        assertThat(captor.getValue().getPayload().getContent()).doesNotContain(": ");
    }
}
