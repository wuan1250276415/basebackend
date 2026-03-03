package com.basebackend.mall.product.consumer;

import com.basebackend.mall.product.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.product.service.ProductService;
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
        consumerGroup = "mall-product-timeout-release-consumer-group"
)
public class OrderTimeoutClosedConsumer extends BaseRocketMQConsumer<OrderTimeoutClosedMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTimeoutClosedConsumer.class);

    private final ProductService productService;

    public OrderTimeoutClosedConsumer(ProductService productService) {
        this.productService = productService;
    }

    @Override
    protected MessageHandler<OrderTimeoutClosedMessage> getMessageHandler() {
        return message -> {
            OrderTimeoutClosedMessage payload = message.getPayload();
            LOGGER.info("收到超时关单消息，释放预占库存，orderNo={}", payload.orderNo());
            productService.releaseReservedStockForTimeoutOrder(payload);
        };
    }

    @Override
    protected Class<OrderTimeoutClosedMessage> getPayloadClass() {
        return OrderTimeoutClosedMessage.class;
    }
}
