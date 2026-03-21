package com.basebackend.mall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.util.SnowflakeIdGenerator;
import com.basebackend.mall.pay.dto.PaymentCreateRequest;
import com.basebackend.mall.pay.entity.MallPayment;
import com.basebackend.mall.pay.enums.MallPaymentStatus;
import com.basebackend.mall.pay.event.MallOrderPayStatus;
import com.basebackend.mall.pay.event.MallOrderStatus;
import com.basebackend.mall.pay.event.OrderCreatedMessage;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Test
    @DisplayName("创建支付单应为同一订单生成不同支付单号")
    void createPaymentShouldGenerateUniquePayNos() {
        PaymentCreateRequest request = new PaymentCreateRequest(
                301L, "TRD202603030001", new BigDecimal("99.00"), "MOCK_CHANNEL");
        when(mallPaymentMapper.insert(any(MallPayment.class))).thenReturn(1);

        try (MockedStatic<SnowflakeIdGenerator> snowflake = Mockito.mockStatic(SnowflakeIdGenerator.class)) {
            snowflake.when(SnowflakeIdGenerator::nextIdStr)
                    .thenReturn("800", "900");

            payService.createPayment(request);
            payService.createPayment(request);
        }

        ArgumentCaptor<MallPayment> paymentCaptor = ArgumentCaptor.forClass(MallPayment.class);
        verify(mallPaymentMapper, times(2)).<MallPayment>insert(paymentCaptor.capture());
        List<MallPayment> capturedPayments = paymentCaptor.getAllValues();
        assertEquals("PAY800", capturedPayments.get(0).getPayNo());
        assertEquals("PAY900", capturedPayments.get(1).getPayNo());
        assertNotEquals(capturedPayments.get(0).getPayNo(), capturedPayments.get(1).getPayNo());
    }

    @Test
    @DisplayName("订单创建事件处理应使用生成器打出 payNo")
    void handleOrderCreatedShouldUseGeneratedPayNo() {
        OrderCreatedMessage message = new OrderCreatedMessage(
                401L, "TRD202603040001", 501L,
                new BigDecimal("149.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 2)));
        when(idempotentStore.tryAcquire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mallPaymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(mallPaymentMapper.insert(any(MallPayment.class))).thenReturn(1);

        try (MockedStatic<SnowflakeIdGenerator> snowflake = Mockito.mockStatic(SnowflakeIdGenerator.class)) {
            snowflake.when(SnowflakeIdGenerator::nextIdStr).thenReturn("1000");
            payService.handleOrderCreated(message);
        }

        ArgumentCaptor<MallPayment> paymentCaptor = ArgumentCaptor.forClass(MallPayment.class);
        verify(mallPaymentMapper).<MallPayment>insert(paymentCaptor.capture());
        assertEquals("PAY1000", paymentCaptor.getValue().getPayNo());
    }

    @Test
    @DisplayName("重复订单创建事件到达时不应重复创建支付单")
    void handleOrderCreatedShouldSkipWhenPaymentAlreadyExists() {
        OrderCreatedMessage message = new OrderCreatedMessage(
                401L, "TRD202603040002", 501L,
                new BigDecimal("149.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 2)));
        MallPayment existingPayment = new MallPayment();
        existingPayment.setId(9001L);
        existingPayment.setOrderNo(message.orderNo());
        existingPayment.setPayNo("PAY_EXISTING_1");

        when(idempotentStore.tryAcquire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mallPaymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existingPayment);

        payService.handleOrderCreated(message);

        verify(mallPaymentMapper, never()).insert(any(MallPayment.class));
    }

    @Test
    @DisplayName("重复订单创建事件未获取幂等锁时应直接跳过")
    void handleOrderCreatedShouldSkipWhenIdempotentLockNotAcquired() {
        OrderCreatedMessage message = new OrderCreatedMessage(
                401L, "TRD202603040003", 501L,
                new BigDecimal("149.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 2)));

        when(idempotentStore.tryAcquire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        payService.handleOrderCreated(message);

        verifyNoInteractions(mallPaymentMapper);
    }
}
