package com.basebackend.mall.pay.consumer;

import com.basebackend.mall.pay.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.pay.service.PayService;
import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订单超时关闭消息消费者
 */
@Component
@RocketMQMessageListener(
        topic = "mall.trade.order-timeout-closed",
        consumerGroup = "mall-pay-timeout-close-consumer-group"
)
public class OrderTimeoutClosedConsumer extends BaseRocketMQConsumer<OrderTimeoutClosedMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTimeoutClosedConsumer.class);

    private final PayService payService;

    public OrderTimeoutClosedConsumer(PayService payService) {
        this.payService = payService;
    }

    @Override
    protected MessageHandler<OrderTimeoutClosedMessage> getMessageHandler() {
        return message -> {
            OrderTimeoutClosedMessage payload = message.getPayload();
            LOGGER.info("收到订单超时关闭消息，关闭支付单，orderNo={}", payload.orderNo());
            payService.handleOrderTimeoutClosed(payload);
        };
    }

    @Override
    protected Class<OrderTimeoutClosedMessage> getPayloadClass() {
        return OrderTimeoutClosedMessage.class;
    }
}
