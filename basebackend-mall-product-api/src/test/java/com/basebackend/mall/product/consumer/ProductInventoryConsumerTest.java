package com.basebackend.mall.product.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.product.event.MallOrderPayStatus;
import com.basebackend.mall.product.event.MallOrderStatus;
import com.basebackend.mall.product.event.OrderCancelledMessage;
import com.basebackend.mall.product.event.OrderItemSnapshot;
import com.basebackend.mall.product.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.product.service.ProductService;
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
@DisplayName("Product 库存消息消费者测试")
class ProductInventoryConsumerTest {

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("订单取消消息应委派到释放预占库存处理")
    void shouldDelegateCancelledOrderMessage() {
        OrderCancelledMessage payload = new OrderCancelledMessage(
                301L,
                "TRD202603030003",
                "PAY_FAILED",
                MallOrderStatus.CANCELLED,
                MallOrderPayStatus.PAY_FAILED,
                List.of(new OrderItemSnapshot(10001L, 2))
        );
        Message<OrderCancelledMessage> message = Message.<OrderCancelledMessage>builder()
                .messageId("msg-cancelled-1")
                .topic("mall.trade.order-cancelled")
                .messageType("ORDER_CANCELLED")
                .tags("ORDER_CANCELLED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new OrderCancelledConsumer(productService).onMessage(JsonUtils.toJsonString(message));

        verify(productService).releaseReservedStockForCancelledOrder(payload);
    }

    @Test
    @DisplayName("超时关单消息应委派到释放预占库存处理")
    void shouldDelegateTimeoutClosedOrderMessage() {
        OrderTimeoutClosedMessage payload = new OrderTimeoutClosedMessage(
                302L,
                "TRD202603030004",
                "TIMEOUT",
                MallOrderStatus.TIMEOUT_CLOSED,
                MallOrderPayStatus.CLOSED,
                List.of(new OrderItemSnapshot(10002L, 1))
        );
        Message<OrderTimeoutClosedMessage> message = Message.<OrderTimeoutClosedMessage>builder()
                .messageId("msg-timeout-1")
                .topic("mall.trade.order-timeout-closed")
                .messageType("ORDER_TIMEOUT_CLOSED")
                .tags("ORDER_TIMEOUT_CLOSED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new OrderTimeoutClosedConsumer(productService).onMessage(JsonUtils.toJsonString(message));

        verify(productService).releaseReservedStockForTimeoutOrder(payload);
    }
}
