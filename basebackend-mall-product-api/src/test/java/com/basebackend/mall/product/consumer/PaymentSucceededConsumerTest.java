package com.basebackend.mall.product.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.product.event.MallPaymentStatus;
import com.basebackend.mall.product.event.PaymentSucceededMessage;
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
@DisplayName("PaymentSucceededConsumer 消息委派测试")
class PaymentSucceededConsumerTest {

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("支付成功消息应委派到商品服务扣减库存")
    void shouldDelegatePaymentSucceededMessageToProductService() {
        PaymentSucceededMessage payload = new PaymentSucceededMessage(
                "PAY202603040012",
                401L,
                "TRD202603040012",
                new BigDecimal("149.00"),
                MallPaymentStatus.PAY_SUCCESS,
                List.of(new PaymentSucceededMessage.PaidItem(10001L, 2))
        );
        Message<PaymentSucceededMessage> message = Message.<PaymentSucceededMessage>builder()
                .messageId("msg-payment-succeeded-product-1")
                .topic("mall.pay.payment-succeeded")
                .messageType("PAYMENT_SUCCEEDED")
                .tags("PAYMENT_SUCCEEDED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new PaymentSucceededConsumer(productService).onMessage(JsonUtils.toJsonString(message));

        ArgumentCaptor<PaymentSucceededMessage> payloadCaptor = ArgumentCaptor.forClass(PaymentSucceededMessage.class);
        verify(productService).deductStockForPaidOrder(payloadCaptor.capture());
        PaymentSucceededMessage actual = payloadCaptor.getValue();
        assertEquals(payload.payNo(), actual.payNo());
        assertEquals(payload.orderId(), actual.orderId());
        assertEquals(payload.orderNo(), actual.orderNo());
        assertEquals(0, payload.payAmount().compareTo(actual.payAmount()));
        assertEquals(payload.paymentStatus(), actual.paymentStatus());
        assertEquals(payload.items(), actual.items());
    }
}
