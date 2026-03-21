package com.basebackend.mall.trade.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.trade.enums.MallPaymentStatus;
import com.basebackend.mall.trade.event.PaymentSucceededMessage;
import com.basebackend.mall.trade.service.TradeService;
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
@DisplayName("Trade PaymentSucceededConsumer 消息委派测试")
class PaymentSucceededConsumerTest {

    @Mock
    private TradeService tradeService;

    @Test
    @DisplayName("支付成功消息应委派到交易服务更新订单状态")
    void shouldDelegatePaymentSucceededMessageToTradeService() {
        PaymentSucceededMessage payload = new PaymentSucceededMessage(
                "PAY202603040013",
                401L,
                "TRD202603040013",
                new BigDecimal("199.00"),
                MallPaymentStatus.PAY_SUCCESS,
                List.of(new PaymentSucceededMessage.PaidItem(10001L, 2))
        );
        Message<PaymentSucceededMessage> message = Message.<PaymentSucceededMessage>builder()
                .messageId("msg-payment-succeeded-trade-1")
                .topic("mall.pay.payment-succeeded")
                .messageType("PAYMENT_SUCCEEDED")
                .tags("PAYMENT_SUCCEEDED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new PaymentSucceededConsumer(tradeService).onMessage(JsonUtils.toJsonString(message));

        ArgumentCaptor<PaymentSucceededMessage> payloadCaptor = ArgumentCaptor.forClass(PaymentSucceededMessage.class);
        verify(tradeService).markOrderPaid(payloadCaptor.capture());
        PaymentSucceededMessage actual = payloadCaptor.getValue();
        assertEquals(payload.payNo(), actual.payNo());
        assertEquals(payload.orderId(), actual.orderId());
        assertEquals(payload.orderNo(), actual.orderNo());
        assertEquals(0, payload.payAmount().compareTo(actual.payAmount()));
        assertEquals(payload.paymentStatus(), actual.paymentStatus());
        assertEquals(payload.items(), actual.items());
    }
}
