package com.basebackend.messaging.management.service;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.entity.DeadLetterEntity;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("死信管理服务测试")
class DeadLetterManagementServiceTest {

    @Mock
    private DeadLetterMapper deadLetterMapper;

    @Mock
    private ObjectProvider<MessageProducer> messageProducerProvider;

    @Mock
    private MessageProducer messageProducer;

    @Captor
    private ArgumentCaptor<DeadLetterEntity> entityCaptor;

    @Captor
    private ArgumentCaptor<Message<Object>> messageCaptor;

    private DeadLetterManagementService deadLetterManagementService;

    @BeforeEach
    void setUp() {
        deadLetterManagementService = new DeadLetterManagementService(deadLetterMapper, messageProducerProvider);
    }

    @Test
    @DisplayName("重投死信时优先按原始消息重建并更新处理状态")
    void redeliverShouldSendOriginalMessageAndUpdateStatus() {
        DeadLetterEntity entity = new DeadLetterEntity();
        entity.setId(1L);
        entity.setStatus("PENDING");
        entity.setMessageId("msg-001");
        entity.setTopic("order.topic");
        entity.setOriginalMessage(JsonUtils.toJsonString(Message.builder()
                .messageId("msg-001")
                .topic("order.topic")
                .routingKey("order.created")
                .payload(Map.of("orderId", 1001))
                .build()));

        when(deadLetterMapper.selectById(1L)).thenReturn(entity);
        when(messageProducerProvider.getIfAvailable()).thenReturn(messageProducer);
        when(messageProducer.send(any(Message.class))).thenReturn("mq-001");

        deadLetterManagementService.redeliver(1L);

        verify(messageProducer).send(messageCaptor.capture());
        verify(deadLetterMapper).updateById(entityCaptor.capture());

        Message<Object> redelivered = messageCaptor.getValue();
        DeadLetterEntity updated = entityCaptor.getValue();

        assertEquals("order.topic", redelivered.getTopic());
        assertEquals("order.created", redelivered.getRoutingKey());
        assertEquals("REDELIVERED", updated.getStatus());
        assertNotNull(updated.getHandledTime());
    }

    @Test
    @DisplayName("查询详情时从原始消息中补齐路由键和请求头")
    void getByIdShouldResolveRoutingKeyAndHeadersFromOriginalMessage() {
        DeadLetterEntity entity = new DeadLetterEntity();
        entity.setId(2L);
        entity.setStatus("PENDING");
        entity.setTopic("invoice.topic");
        entity.setPayload("{\"invoiceId\":8}");
        entity.setOriginalMessage(JsonUtils.toJsonString(Message.builder()
                .topic("invoice.topic")
                .routingKey("invoice.created")
                .headers(Map.of("x-trace-id", "trace-1"))
                .build()));

        when(deadLetterMapper.selectById(2L)).thenReturn(entity);

        var view = deadLetterManagementService.getById(2L);

        assertNotNull(view);
        assertEquals("invoice.created", view.getRoutingKey());
        assertTrue(view.getHeaders().contains("x-trace-id"));
    }
}
