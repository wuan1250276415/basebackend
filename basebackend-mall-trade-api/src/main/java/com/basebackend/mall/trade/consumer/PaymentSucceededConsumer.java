package com.basebackend.mall.trade.consumer;

import com.basebackend.mall.trade.event.PaymentSucceededMessage;
import com.basebackend.mall.trade.service.TradeService;
import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 支付成功消息消费者
 */
@Component
@RocketMQMessageListener(
        topic = "mall.pay.payment-succeeded",
        consumerGroup = "mall-trade-payment-consumer-group"
)
public class PaymentSucceededConsumer extends BaseRocketMQConsumer<PaymentSucceededMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentSucceededConsumer.class);

    private final TradeService tradeService;

    public PaymentSucceededConsumer(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Override
    protected MessageHandler<PaymentSucceededMessage> getMessageHandler() {
        return message -> {
            PaymentSucceededMessage payload = message.getPayload();
            LOGGER.info("收到支付成功消息，更新订单状态，orderNo={}", payload.orderNo());
            tradeService.markOrderPaid(payload);
        };
    }

    @Override
    protected Class<PaymentSucceededMessage> getPayloadClass() {
        return PaymentSucceededMessage.class;
    }
}
