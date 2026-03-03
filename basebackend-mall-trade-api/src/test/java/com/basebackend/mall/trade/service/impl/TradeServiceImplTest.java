package com.basebackend.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.mall.trade.entity.MallOrder;
import com.basebackend.mall.trade.entity.MallOrderItem;
import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;
import com.basebackend.mall.trade.enums.MallPaymentStatus;
import com.basebackend.mall.trade.event.OrderCancelledMessage;
import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.event.TradeEventTopics;
import com.basebackend.mall.trade.mapper.MallOrderItemMapper;
import com.basebackend.mall.trade.mapper.MallOrderMapper;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TradeServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TradeServiceImpl 单元测试")
class TradeServiceImplTest {

    @Mock
    private MallOrderMapper mallOrderMapper;

    @Mock
    private MallOrderItemMapper mallOrderItemMapper;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private IdempotentStore idempotentStore;

    @InjectMocks
    private TradeServiceImpl tradeService;

    @Test
    @DisplayName("支付失败回滚时应取消订单并发送取消事件")
    void shouldCancelOrderAndPublishOrderCancelledWhenPaymentFailed() {
        MallOrder mallOrder = new MallOrder();
        mallOrder.setId(101L);
        mallOrder.setOrderNo("TRD202603030001");
        mallOrder.setOrderStatus(MallOrderStatus.CREATED.getCode());
        mallOrder.setPayStatus(MallOrderPayStatus.UNPAID.getCode());

        MallOrderItem orderItem = new MallOrderItem();
        orderItem.setOrderNo(mallOrder.getOrderNo());
        orderItem.setSkuId(10001L);
        orderItem.setQuantity(2);

        when(mallOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mallOrder);
        when(mallOrderItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(orderItem));
        when(messageProducer.send(any())).thenReturn("msg-id");
        when(idempotentStore.tryAcquire(any(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        PaymentFailedMessage message = new PaymentFailedMessage(
                "PAY202603030001",
                mallOrder.getId(),
                mallOrder.getOrderNo(),
                MallPaymentStatus.PAY_FAILED,
                "MOCK_PAY_FAILED"
        );

        tradeService.markOrderPaymentFailed(message);

        ArgumentCaptor<MallOrder> orderCaptor = ArgumentCaptor.forClass(MallOrder.class);
        verify(mallOrderMapper).updateById(orderCaptor.capture());
        MallOrder updatedOrder = orderCaptor.getValue();
        assertEquals(MallOrderPayStatus.PAY_FAILED.getCode(), updatedOrder.getPayStatus());
        assertEquals(MallOrderStatus.CANCELLED.getCode(), updatedOrder.getOrderStatus());
        assertNotNull(updatedOrder.getCloseTime());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).send(messageCaptor.capture());
        Message<?> sentMessage = messageCaptor.getValue();
        assertEquals(TradeEventTopics.ORDER_CANCELLED, sentMessage.getTopic());
        assertEquals("ORDER_CANCELLED", sentMessage.getMessageType());
        assertEquals("ORDER_CANCELLED", sentMessage.getTags());

        assertTrue(sentMessage.getPayload() instanceof OrderCancelledMessage);
        OrderCancelledMessage payload = (OrderCancelledMessage) sentMessage.getPayload();
        assertEquals(mallOrder.getOrderNo(), payload.orderNo());
        assertEquals("MOCK_PAY_FAILED", payload.reason());
        assertEquals(1, payload.items().size());
        assertEquals(10001L, payload.items().get(0).skuId());
        assertEquals(2, payload.items().get(0).quantity());
    }
}
