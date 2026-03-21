package com.basebackend.mall.product.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.product.event.MallOrderPayStatus;
import com.basebackend.mall.product.event.MallOrderStatus;
import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.service.ProductService;
import com.basebackend.messaging.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreatedConsumer 消息委派测试")
class OrderCreatedConsumerTest {

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("下单消息应委派到商品服务预占库存")
    void shouldDelegateOrderCreatedMessageToProductService() {
        OrderCreatedMessage payload = new OrderCreatedMessage(
                501L,
                "TRD202603040020",
                601L,
                new BigDecimal("259.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(
                        new OrderCreatedMessage.OrderItem(10001L, 2),
                        new OrderCreatedMessage.OrderItem(10002L, 1)
                )
        );
        Message<OrderCreatedMessage> message = Message.<OrderCreatedMessage>builder()
                .messageId("msg-order-created-product-1")
                .topic("mall.trade.order-created")
                .messageType("ORDER_CREATED")
                .tags("ORDER_CREATED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new OrderCreatedConsumer(productService).onMessage(JsonUtils.toJsonString(message));

        ArgumentCaptor<OrderCreatedMessage> payloadCaptor = ArgumentCaptor.forClass(OrderCreatedMessage.class);
        verify(productService).reserveStockForOrder(payloadCaptor.capture());
        OrderCreatedMessage actual = payloadCaptor.getValue();
        assertEquals(payload.orderId(), actual.orderId());
        assertEquals(payload.orderNo(), actual.orderNo());
        assertEquals(payload.userId(), actual.userId());
        assertEquals(0, payload.payAmount().compareTo(actual.payAmount()));
        assertEquals(payload.orderStatus(), actual.orderStatus());
        assertEquals(payload.orderPayStatus(), actual.orderPayStatus());
        assertEquals(payload.items(), actual.items());
    }
}
