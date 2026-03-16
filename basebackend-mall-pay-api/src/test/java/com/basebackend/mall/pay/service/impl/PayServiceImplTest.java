package com.basebackend.mall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.mall.pay.entity.MallPayment;
import com.basebackend.mall.pay.enums.MallPaymentStatus;
import com.basebackend.mall.pay.event.PayEventTopics;
import com.basebackend.mall.pay.event.PaymentSucceededMessage;
import com.basebackend.mall.pay.mapper.MallPaymentMapper;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PayServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PayServiceImpl 单元测试")
class PayServiceImplTest {

    @Mock
    private MallPaymentMapper mallPaymentMapper;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private IdempotentStore idempotentStore;

    @InjectMocks
    private PayServiceImpl payService;

    @Test
    @DisplayName("模拟支付成功应更新支付单并发送支付成功事件")
    void shouldMarkPaymentSuccessAndPublishSucceededEvent() {
        MallPayment mallPayment = new MallPayment();
        mallPayment.setId(201L);
        mallPayment.setPayNo("PAY202603030001");
        mallPayment.setOrderId(301L);
        mallPayment.setOrderNo("TRD202603030001");
        mallPayment.setPayStatus(MallPaymentStatus.WAIT_PAY.getCode());
        mallPayment.setPayAmount(new BigDecimal("99.00"));
        mallPayment.setOrderItemsJson("[{\"skuId\":10001,\"quantity\":2}]");

        when(mallPaymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mallPayment);
        when(messageProducer.send(any())).thenReturn("msg-id");

        payService.mockPaySuccess(mallPayment.getOrderNo());

        ArgumentCaptor<MallPayment> paymentCaptor = ArgumentCaptor.forClass(MallPayment.class);
        verify(mallPaymentMapper).updateById(paymentCaptor.capture());
        MallPayment updatedPayment = paymentCaptor.getValue();
        assertEquals(MallPaymentStatus.PAY_SUCCESS.getCode(), updatedPayment.getPayStatus());
        assertTrue(updatedPayment.getThirdPartyTradeNo().startsWith("MOCK-"));
        assertNotNull(updatedPayment.getPaidTime());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).send(messageCaptor.capture());
        Message<?> sentMessage = messageCaptor.getValue();
        assertEquals(PayEventTopics.PAYMENT_SUCCEEDED, sentMessage.getTopic());
        assertEquals("PAYMENT_SUCCEEDED", sentMessage.getMessageType());
        assertEquals("PAYMENT_SUCCEEDED", sentMessage.getTags());

        assertTrue(sentMessage.getPayload() instanceof PaymentSucceededMessage);
        PaymentSucceededMessage payload = (PaymentSucceededMessage) sentMessage.getPayload();
        assertEquals(mallPayment.getOrderNo(), payload.orderNo());
        assertEquals(mallPayment.getPayNo(), payload.payNo());
        assertEquals(1, payload.items().size());
        assertEquals(10001L, payload.items().get(0).skuId());
        assertEquals(2, payload.items().get(0).quantity());
        assertFalse(payload.items().isEmpty());
    }
}
