package com.basebackend.mall.pay.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.pay.event.MallOrderPayStatus;
import com.basebackend.mall.pay.event.MallOrderStatus;
import com.basebackend.mall.pay.event.OrderItemSnapshot;
import com.basebackend.mall.pay.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.pay.service.PayService;
import com.basebackend.messaging.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderTimeoutClosedConsumer 消息委派测试")
class OrderTimeoutClosedConsumerTest {

    @Mock
    private PayService payService;

    @Test
    @DisplayName("订单超时关闭消息应委派到支付服务关闭支付单")
    void shouldDelegateTimeoutClosedOrderToPayService() {
        OrderTimeoutClosedMessage payload = new OrderTimeoutClosedMessage(
                401L,
                "TRD202603040011",
                "TIMEOUT",
                MallOrderStatus.TIMEOUT_CLOSED,
                MallOrderPayStatus.CLOSED,
                List.of(new OrderItemSnapshot(10001L, 2))
        );
        Message<OrderTimeoutClosedMessage> message = Message.<OrderTimeoutClosedMessage>builder()
                .messageId("msg-timeout-close-1")
                .topic("mall.trade.order-timeout-closed")
                .messageType("ORDER_TIMEOUT_CLOSED")
                .tags("ORDER_TIMEOUT_CLOSED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new OrderTimeoutClosedConsumer(payService).onMessage(JsonUtils.toJsonString(message));

        verify(payService).handleOrderTimeoutClosed(payload);
    }
}
