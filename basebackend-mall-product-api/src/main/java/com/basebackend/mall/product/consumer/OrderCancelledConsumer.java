package com.basebackend.mall.product.consumer;

import com.basebackend.mall.product.event.OrderCancelledMessage;
import com.basebackend.mall.product.service.ProductService;
import com.basebackend.messaging.consumer.BaseRocketMQConsumer;
import com.basebackend.messaging.handler.MessageHandler;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 订单取消消息消费者
 */
@Component
@RocketMQMessageListener(
        topic = "mall.trade.order-cancelled",
        consumerGroup = "mall-product-cancel-release-consumer-group"
)
public class OrderCancelledConsumer extends BaseRocketMQConsumer<OrderCancelledMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCancelledConsumer.class);

    private final ProductService productService;

    public OrderCancelledConsumer(ProductService productService) {
        this.productService = productService;
    }

    @Override
    protected MessageHandler<OrderCancelledMessage> getMessageHandler() {
        return message -> {
            OrderCancelledMessage payload = message.getPayload();
            LOGGER.info("收到订单取消消息，释放预占库存，orderNo={}", payload.orderNo());
            productService.releaseReservedStockForCancelledOrder(payload);
        };
    }

    @Override
    protected Class<OrderCancelledMessage> getPayloadClass() {
        return OrderCancelledMessage.class;
    }
}
