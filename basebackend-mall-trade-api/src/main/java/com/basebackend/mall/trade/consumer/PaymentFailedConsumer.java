package com.basebackend.mall.trade.consumer;

import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.service.TradeService;
import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 支付失败消息消费者
 */
@Component
@RocketMQMessageListener(
        topic = "mall.pay.payment-failed",
        consumerGroup = "mall-trade-payment-failed-consumer-group"
)
public class PaymentFailedConsumer extends BaseRocketMQConsumer<PaymentFailedMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentFailedConsumer.class);

    private final TradeService tradeService;

    public PaymentFailedConsumer(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Override
    protected MessageHandler<PaymentFailedMessage> getMessageHandler() {
        return message -> {
            PaymentFailedMessage payload = message.getPayload();
            LOGGER.info("收到支付失败消息，触发订单回滚，orderNo={}", payload.orderNo());
            tradeService.markOrderPaymentFailed(payload);
        };
    }

    @Override
    protected Class<PaymentFailedMessage> getPayloadClass() {
        return PaymentFailedMessage.class;
    }
}
