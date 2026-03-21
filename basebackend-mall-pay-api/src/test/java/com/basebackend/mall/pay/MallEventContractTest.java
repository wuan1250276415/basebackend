package com.basebackend.mall.pay;

import com.basebackend.api.model.product.ProductDetailDTO;
import com.basebackend.common.context.UserContext;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.model.Result;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.pay.consumer.OrderCreatedConsumer;
import com.basebackend.mall.pay.entity.MallPayment;
import com.basebackend.mall.pay.enums.MallPaymentStatus;
import com.basebackend.mall.pay.mapper.MallPaymentMapper;
import com.basebackend.mall.pay.service.PayService;
import com.basebackend.mall.pay.service.impl.PayServiceImpl;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.service.client.ProductServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("商城事件跨模块契约测试")
class MallEventContractTest {

    @Mock
    private com.basebackend.mall.trade.mapper.MallOrderMapper tradeOrderMapper;

    @Mock
    private com.basebackend.mall.trade.mapper.MallOrderItemMapper tradeOrderItemMapper;

    @Mock
    private MessageProducer tradeMessageProducer;

    @Mock
    private IdempotentStore tradeIdempotentStore;

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private MallPaymentMapper mallPaymentMapper;

    @Mock
    private MessageProducer payMessageProducer;

    @Mock
    private IdempotentStore payIdempotentStore;

    @Mock
    private PayService payService;

    @Mock
    private com.basebackend.mall.trade.service.TradeService tradeService;

    @Mock
    private com.basebackend.mall.product.service.ProductService productService;

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("Trade 发出的订单创建事件应能被 Pay 与 Product 消费者兼容解析")
    void shouldDeliverTradeOrderCreatedEventToPayAndProductConsumers() {
        com.basebackend.mall.trade.service.impl.TradeServiceImpl upstreamTradeService =
                new com.basebackend.mall.trade.service.impl.TradeServiceImpl(
                        tradeOrderMapper,
                        tradeOrderItemMapper,
                        tradeMessageProducer,
                        tradeIdempotentStore,
                        productServiceClient
                );
        UserContextHolder.set(UserContext.builder().userId(42L).build());

        when(productServiceClient.getProductDetail(10001L)).thenReturn(Result.success(
                new ProductDetailDTO(10001L, 20001L, "SKU-10001", "BaseBackend 企业级网关实战",
                        new BigDecimal("99.00"), 500, true)
        ));
        when(productServiceClient.getProductDetail(10002L)).thenReturn(Result.success(
                new ProductDetailDTO(10002L, 20002L, "SKU-10002", "分布式缓存与一致性实践",
                        new BigDecimal("129.00"), 300, true)
        ));
        when(tradeOrderMapper.insert(any(com.basebackend.mall.trade.entity.MallOrder.class))).thenAnswer(invocation -> {
            com.basebackend.mall.trade.entity.MallOrder order = invocation.getArgument(0);
            order.setId(2001L);
            return 1;
        });

        com.basebackend.mall.trade.dto.OrderSubmitRequest request = new com.basebackend.mall.trade.dto.OrderSubmitRequest(
                List.of(
                        new com.basebackend.mall.trade.dto.OrderSubmitRequest.OrderItem(10001L, 2),
                        new com.basebackend.mall.trade.dto.OrderSubmitRequest.OrderItem(10002L, 1)
                )
        );

        upstreamTradeService.submitOrder(request);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(tradeMessageProducer).send(messageCaptor.capture());
        Message<?> upstreamMessage = messageCaptor.getValue();
        assertEquals("mall.trade.order-created", upstreamMessage.getTopic());
        assertEquals("ORDER_CREATED", upstreamMessage.getMessageType());
        assertEquals("ORDER_CREATED", upstreamMessage.getTags());
        assertTrue(upstreamMessage.getPayload() instanceof com.basebackend.mall.trade.event.OrderCreatedMessage);

        com.basebackend.mall.trade.event.OrderCreatedMessage upstreamPayload =
                (com.basebackend.mall.trade.event.OrderCreatedMessage) upstreamMessage.getPayload();
        String messageJson = JsonUtils.toJsonString(upstreamMessage);

        new OrderCreatedConsumer(payService).onMessage(messageJson);
        ArgumentCaptor<com.basebackend.mall.pay.event.OrderCreatedMessage> payPayloadCaptor =
                ArgumentCaptor.forClass(com.basebackend.mall.pay.event.OrderCreatedMessage.class);
        verify(payService).handleOrderCreated(payPayloadCaptor.capture());
        assertPayOrderCreatedPayload(upstreamPayload, payPayloadCaptor.getValue());

        new com.basebackend.mall.product.consumer.OrderCreatedConsumer(productService).onMessage(messageJson);
        ArgumentCaptor<com.basebackend.mall.product.event.OrderCreatedMessage> productPayloadCaptor =
                ArgumentCaptor.forClass(com.basebackend.mall.product.event.OrderCreatedMessage.class);
        verify(productService).reserveStockForOrder(productPayloadCaptor.capture());
        assertProductOrderCreatedPayload(upstreamPayload, productPayloadCaptor.getValue());
    }

    @Test
    @DisplayName("Pay 发出的支付成功事件应能被 Trade 与 Product 消费者兼容解析")
    void shouldDeliverPayPaymentSucceededEventToTradeAndProductConsumers() {
        PayServiceImpl upstreamPayService = new PayServiceImpl(mallPaymentMapper, payMessageProducer, payIdempotentStore);
        MallPayment mallPayment = createPendingPayment("PAY202603200001", "TRD202603200001");
        mallPayment.setPayAmount(new BigDecimal("149.00"));
        mallPayment.setOrderItemsJson("[{\"skuId\":10001,\"quantity\":2},{\"skuId\":10002,\"quantity\":1}]");
        when(mallPaymentMapper.selectOne(any())).thenReturn(mallPayment);

        upstreamPayService.mockPaySuccess(mallPayment.getOrderNo());

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(payMessageProducer).send(messageCaptor.capture());
        Message<?> upstreamMessage = messageCaptor.getValue();
        assertEquals("mall.pay.payment-succeeded", upstreamMessage.getTopic());
        assertEquals("PAYMENT_SUCCEEDED", upstreamMessage.getMessageType());
        assertEquals("PAYMENT_SUCCEEDED", upstreamMessage.getTags());
        assertTrue(upstreamMessage.getPayload() instanceof com.basebackend.mall.pay.event.PaymentSucceededMessage);

        com.basebackend.mall.pay.event.PaymentSucceededMessage upstreamPayload =
                (com.basebackend.mall.pay.event.PaymentSucceededMessage) upstreamMessage.getPayload();
        String messageJson = JsonUtils.toJsonString(upstreamMessage);

        new com.basebackend.mall.trade.consumer.PaymentSucceededConsumer(tradeService).onMessage(messageJson);
        ArgumentCaptor<com.basebackend.mall.trade.event.PaymentSucceededMessage> tradePayloadCaptor =
                ArgumentCaptor.forClass(com.basebackend.mall.trade.event.PaymentSucceededMessage.class);
        verify(tradeService).markOrderPaid(tradePayloadCaptor.capture());
        assertTradePaymentSucceededPayload(upstreamPayload, tradePayloadCaptor.getValue());

        new com.basebackend.mall.product.consumer.PaymentSucceededConsumer(productService).onMessage(messageJson);
        ArgumentCaptor<com.basebackend.mall.product.event.PaymentSucceededMessage> productPayloadCaptor =
                ArgumentCaptor.forClass(com.basebackend.mall.product.event.PaymentSucceededMessage.class);
        verify(productService).deductStockForPaidOrder(productPayloadCaptor.capture());
        assertProductPaymentSucceededPayload(upstreamPayload, productPayloadCaptor.getValue());
    }

    @Test
    @DisplayName("Pay 发出的支付失败事件应能被 Trade 消费者兼容解析")
    void shouldDeliverPayPaymentFailedEventToTradeConsumer() {
        PayServiceImpl upstreamPayService = new PayServiceImpl(mallPaymentMapper, payMessageProducer, payIdempotentStore);
        MallPayment mallPayment = createPendingPayment("PAY202603200002", "TRD202603200002");
        when(mallPaymentMapper.selectOne(any())).thenReturn(mallPayment);

        upstreamPayService.mockPayFail(mallPayment.getOrderNo(), "CHANNEL_TIMEOUT");

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(payMessageProducer).send(messageCaptor.capture());
        Message<?> upstreamMessage = messageCaptor.getValue();
        assertEquals("mall.pay.payment-failed", upstreamMessage.getTopic());
        assertEquals("PAYMENT_FAILED", upstreamMessage.getMessageType());
        assertEquals("PAYMENT_FAILED", upstreamMessage.getTags());
        assertTrue(upstreamMessage.getPayload() instanceof com.basebackend.mall.pay.event.PaymentFailedMessage);

        com.basebackend.mall.pay.event.PaymentFailedMessage upstreamPayload =
                (com.basebackend.mall.pay.event.PaymentFailedMessage) upstreamMessage.getPayload();
        String messageJson = JsonUtils.toJsonString(upstreamMessage);

        new com.basebackend.mall.trade.consumer.PaymentFailedConsumer(tradeService).onMessage(messageJson);
        ArgumentCaptor<com.basebackend.mall.trade.event.PaymentFailedMessage> tradePayloadCaptor =
                ArgumentCaptor.forClass(com.basebackend.mall.trade.event.PaymentFailedMessage.class);
        verify(tradeService).markOrderPaymentFailed(tradePayloadCaptor.capture());
        assertTradePaymentFailedPayload(upstreamPayload, tradePayloadCaptor.getValue());
    }

    private MallPayment createPendingPayment(String payNo, String orderNo) {
        MallPayment mallPayment = new MallPayment();
        mallPayment.setId(3001L);
        mallPayment.setPayNo(payNo);
        mallPayment.setOrderId(4001L);
        mallPayment.setOrderNo(orderNo);
        mallPayment.setPayStatus(MallPaymentStatus.WAIT_PAY.getCode());
        mallPayment.setPayAmount(new BigDecimal("99.00"));
        mallPayment.setOrderItemsJson("[{\"skuId\":10001,\"quantity\":2}]");
        return mallPayment;
    }

    private void assertPayOrderCreatedPayload(com.basebackend.mall.trade.event.OrderCreatedMessage expected,
                                              com.basebackend.mall.pay.event.OrderCreatedMessage actual) {
        assertEquals(expected.orderId(), actual.orderId());
        assertEquals(expected.orderNo(), actual.orderNo());
        assertEquals(expected.userId(), actual.userId());
        assertEquals(0, expected.payAmount().compareTo(actual.payAmount()));
        assertEquals(expected.orderStatus().name(), actual.orderStatus().name());
        assertEquals(expected.orderPayStatus().name(), actual.orderPayStatus().name());
        assertEquals(expected.items().size(), actual.items().size());
        assertEquals(expected.items().get(0).skuId(), actual.items().get(0).skuId());
        assertEquals(expected.items().get(0).quantity(), actual.items().get(0).quantity());
        assertEquals(expected.items().get(1).skuId(), actual.items().get(1).skuId());
        assertEquals(expected.items().get(1).quantity(), actual.items().get(1).quantity());
    }

    private void assertProductOrderCreatedPayload(com.basebackend.mall.trade.event.OrderCreatedMessage expected,
                                                  com.basebackend.mall.product.event.OrderCreatedMessage actual) {
        assertEquals(expected.orderId(), actual.orderId());
        assertEquals(expected.orderNo(), actual.orderNo());
        assertEquals(expected.userId(), actual.userId());
        assertEquals(0, expected.payAmount().compareTo(actual.payAmount()));
        assertEquals(expected.orderStatus().name(), actual.orderStatus().name());
        assertEquals(expected.orderPayStatus().name(), actual.orderPayStatus().name());
        assertEquals(expected.items().size(), actual.items().size());
        assertEquals(expected.items().get(0).skuId(), actual.items().get(0).skuId());
        assertEquals(expected.items().get(0).quantity(), actual.items().get(0).quantity());
        assertEquals(expected.items().get(1).skuId(), actual.items().get(1).skuId());
        assertEquals(expected.items().get(1).quantity(), actual.items().get(1).quantity());
    }

    private void assertTradePaymentSucceededPayload(com.basebackend.mall.pay.event.PaymentSucceededMessage expected,
                                                    com.basebackend.mall.trade.event.PaymentSucceededMessage actual) {
        assertEquals(expected.payNo(), actual.payNo());
        assertEquals(expected.orderId(), actual.orderId());
        assertEquals(expected.orderNo(), actual.orderNo());
        assertEquals(0, expected.payAmount().compareTo(actual.payAmount()));
        assertEquals(expected.paymentStatus().name(), actual.paymentStatus().name());
        assertEquals(expected.items().size(), actual.items().size());
        assertEquals(expected.items().get(0).skuId(), actual.items().get(0).skuId());
        assertEquals(expected.items().get(0).quantity(), actual.items().get(0).quantity());
        assertEquals(expected.items().get(1).skuId(), actual.items().get(1).skuId());
        assertEquals(expected.items().get(1).quantity(), actual.items().get(1).quantity());
    }

    private void assertProductPaymentSucceededPayload(com.basebackend.mall.pay.event.PaymentSucceededMessage expected,
                                                      com.basebackend.mall.product.event.PaymentSucceededMessage actual) {
        assertEquals(expected.payNo(), actual.payNo());
        assertEquals(expected.orderId(), actual.orderId());
        assertEquals(expected.orderNo(), actual.orderNo());
        assertEquals(0, expected.payAmount().compareTo(actual.payAmount()));
        assertEquals(expected.paymentStatus().name(), actual.paymentStatus().name());
        assertEquals(expected.items().size(), actual.items().size());
        assertEquals(expected.items().get(0).skuId(), actual.items().get(0).skuId());
        assertEquals(expected.items().get(0).quantity(), actual.items().get(0).quantity());
        assertEquals(expected.items().get(1).skuId(), actual.items().get(1).skuId());
        assertEquals(expected.items().get(1).quantity(), actual.items().get(1).quantity());
    }

    private void assertTradePaymentFailedPayload(com.basebackend.mall.pay.event.PaymentFailedMessage expected,
                                                 com.basebackend.mall.trade.event.PaymentFailedMessage actual) {
        assertEquals(expected.payNo(), actual.payNo());
        assertEquals(expected.orderId(), actual.orderId());
        assertEquals(expected.orderNo(), actual.orderNo());
        assertEquals(expected.paymentStatus().name(), actual.paymentStatus().name());
        assertEquals(expected.reason(), actual.reason());
    }
}
