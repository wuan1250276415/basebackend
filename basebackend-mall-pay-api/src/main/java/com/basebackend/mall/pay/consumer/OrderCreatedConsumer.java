package com.basebackend.mall.pay.consumer;

import com.basebackend.mall.pay.event.OrderCreatedMessage;
import com.basebackend.mall.pay.service.PayService;
import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息消费者
 */
@Component
@RocketMQMessageListener(
        topic = "mall.trade.order-created",
        consumerGroup = "mall-pay-order-created-consumer-group"
)
public class OrderCreatedConsumer extends BaseRocketMQConsumer<OrderCreatedMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final PayService payService;

    public OrderCreatedConsumer(PayService payService) {
        this.payService = payService;
    }

    @Override
    protected MessageHandler<OrderCreatedMessage> getMessageHandler() {
        return message -> {
            OrderCreatedMessage payload = message.getPayload();
            LOGGER.info("收到订单创建消息，开始创建支付单，orderNo={}", payload.orderNo());
            payService.handleOrderCreated(payload);
        };
    }

    @Override
    protected Class<OrderCreatedMessage> getPayloadClass() {
        return OrderCreatedMessage.class;
    }
}
