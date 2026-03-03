package com.basebackend.mall.product.consumer;

import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.service.ProductService;
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
        consumerGroup = "mall-product-reserve-consumer-group"
)
public class OrderCreatedConsumer extends BaseRocketMQConsumer<OrderCreatedMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductService productService;

    public OrderCreatedConsumer(ProductService productService) {
        this.productService = productService;
    }

    @Override
    protected MessageHandler<OrderCreatedMessage> getMessageHandler() {
        return message -> {
            OrderCreatedMessage payload = message.getPayload();
            LOGGER.info("收到下单消息，开始预占库存，orderNo={}", payload.orderNo());
            productService.reserveStockForOrder(payload);
        };
    }

    @Override
    protected Class<OrderCreatedMessage> getPayloadClass() {
        return OrderCreatedMessage.class;
    }
}
