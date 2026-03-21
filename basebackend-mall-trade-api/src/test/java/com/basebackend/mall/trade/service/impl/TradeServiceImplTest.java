package com.basebackend.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.api.model.product.ProductDetailDTO;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.exception.BusinessException;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.model.Result;
import com.basebackend.mall.trade.dto.OrderSubmitRequest;
import com.basebackend.mall.trade.dto.OrderSubmitResponse;
import com.basebackend.mall.trade.entity.MallOrder;
import com.basebackend.mall.trade.entity.MallOrderItem;
import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;
import com.basebackend.mall.trade.enums.MallPaymentStatus;
import com.basebackend.mall.trade.event.OrderCancelledMessage;
import com.basebackend.mall.trade.event.OrderCreatedMessage;
import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.event.TradeEventTopics;
import com.basebackend.mall.trade.mapper.MallOrderItemMapper;
import com.basebackend.mall.trade.mapper.MallOrderMapper;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.service.client.ProductServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private TradeServiceImpl tradeService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("提交订单时应使用当前登录用户并按商品服务售价核价")
    void shouldSubmitOrderUsingAuthenticatedUserAndServerSidePricing() {
        UserContextHolder.set(UserContext.builder().userId(42L).build());

        when(productServiceClient.getProductDetail(10001L)).thenReturn(Result.success(
                new ProductDetailDTO(10001L, 20001L, "SKU-10001", "BaseBackend 企业级网关实战",
                        new BigDecimal("99.00"), 500, true)
        ));
        when(productServiceClient.getProductDetail(10002L)).thenReturn(Result.success(
                new ProductDetailDTO(10002L, 20002L, "SKU-10002", "分布式缓存与一致性实践",
                        new BigDecimal("129.00"), 300, true)
        ));
        when(mallOrderMapper.insert(any(MallOrder.class))).thenAnswer(invocation -> {
            MallOrder order = invocation.getArgument(0);
            order.setId(2001L);
            return 1;
        });

        OrderSubmitRequest request = new OrderSubmitRequest(List.of(
                new OrderSubmitRequest.OrderItem(10001L, 2),
                new OrderSubmitRequest.OrderItem(10002L, 1)
        ));

        OrderSubmitResponse response = tradeService.submitOrder(request);

        ArgumentCaptor<MallOrder> orderCaptor = ArgumentCaptor.forClass(MallOrder.class);
        verify(mallOrderMapper).insert(orderCaptor.capture());
        MallOrder createdOrder = orderCaptor.getValue();
        assertEquals(42L, createdOrder.getUserId());
        assertEquals(new BigDecimal("327.00"), createdOrder.getTotalAmount());
        assertEquals(new BigDecimal("327.00"), createdOrder.getPayAmount());
        assertThat(createdOrder.getOrderNo()).startsWith("TRD");

        ArgumentCaptor<MallOrderItem> orderItemCaptor = ArgumentCaptor.forClass(MallOrderItem.class);
        verify(mallOrderItemMapper, times(2)).insert(orderItemCaptor.capture());
        List<MallOrderItem> insertedItems = orderItemCaptor.getAllValues();
        assertThat(insertedItems)
                .extracting(MallOrderItem::getSkuId, MallOrderItem::getSkuName,
                        MallOrderItem::getUnitPrice, MallOrderItem::getQuantity, MallOrderItem::getLineAmount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10001L, "BaseBackend 企业级网关实战",
                                new BigDecimal("99.00"), 2, new BigDecimal("198.00")),
                        org.assertj.core.groups.Tuple.tuple(10002L, "分布式缓存与一致性实践",
                                new BigDecimal("129.00"), 1, new BigDecimal("129.00"))
                );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageProducer).send(messageCaptor.capture());
        Message<?> sentMessage = messageCaptor.getValue();
        assertTrue(sentMessage.getPayload() instanceof OrderCreatedMessage);
        OrderCreatedMessage payload = (OrderCreatedMessage) sentMessage.getPayload();
        assertEquals(42L, payload.userId());
        assertEquals(new BigDecimal("327.00"), payload.payAmount());
        assertEquals(2, payload.items().size());

        assertEquals(createdOrder.getOrderNo(), response.orderNo());
        assertEquals(MallOrderStatus.CREATED, response.orderStatus());
        assertEquals(new BigDecimal("327.00"), response.payAmount());
    }

    @Test
    @DisplayName("未登录用户提交订单时应拒绝处理")
    void shouldRejectSubmitOrderWhenUserContextMissing() {
        OrderSubmitRequest request = new OrderSubmitRequest(List.of(
                new OrderSubmitRequest.OrderItem(10001L, 1)
        ));

        assertThatThrownBy(() -> tradeService.submitOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("未授权访问");

        verifyNoInteractions(productServiceClient);
    }

    @Test
    @DisplayName("商品服务核价失败时应拒绝下单且不落库")
    void shouldRejectSubmitOrderWhenProductServiceReturnsFailure() {
        UserContextHolder.set(UserContext.builder().userId(42L).build());
        when(productServiceClient.getProductDetail(10001L)).thenReturn(Result.error("pricing service degraded"));

        OrderSubmitRequest request = new OrderSubmitRequest(List.of(
                new OrderSubmitRequest.OrderItem(10001L, 1)
        ));

        assertThatThrownBy(() -> tradeService.submitOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("商品服务核价失败")
                .hasMessageContaining("10001");

        verify(productServiceClient).getProductDetail(10001L);
        verifyNoInteractions(mallOrderMapper, mallOrderItemMapper, messageProducer);
    }

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
