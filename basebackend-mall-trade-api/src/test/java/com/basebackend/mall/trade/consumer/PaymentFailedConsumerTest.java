package com.basebackend.mall.trade.consumer;

import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.trade.enums.MallPaymentStatus;
import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.service.TradeService;
import com.basebackend.messaging.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trade PaymentFailedConsumer 消息委派测试")
class PaymentFailedConsumerTest {

    @Mock
    private TradeService tradeService;

    @Test
    @DisplayName("支付失败消息应委派到交易服务执行订单回滚")
    void shouldDelegatePaymentFailedMessageToTradeService() {
        PaymentFailedMessage payload = new PaymentFailedMessage(
                "PAY202603040021",
                501L,
                "TRD202603040021",
                MallPaymentStatus.PAY_FAILED,
                "MOCK_PAY_FAILED"
        );
        Message<PaymentFailedMessage> message = Message.<PaymentFailedMessage>builder()
                .messageId("msg-payment-failed-trade-1")
                .topic("mall.pay.payment-failed")
                .messageType("PAYMENT_FAILED")
                .tags("PAYMENT_FAILED")
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();

        new PaymentFailedConsumer(tradeService).onMessage(JsonUtils.toJsonString(message));

        verify(tradeService).markOrderPaymentFailed(payload);
    }
}
