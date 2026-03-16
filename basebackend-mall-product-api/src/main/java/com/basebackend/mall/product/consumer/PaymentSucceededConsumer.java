package com.basebackend.mall.product.consumer;

import com.basebackend.mall.product.event.PaymentSucceededMessage;
import com.basebackend.mall.product.service.ProductService;
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
        consumerGroup = "mall-product-stock-consumer-group"
)
public class PaymentSucceededConsumer extends BaseRocketMQConsumer<PaymentSucceededMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentSucceededConsumer.class);

    private final ProductService productService;

    public PaymentSucceededConsumer(ProductService productService) {
        this.productService = productService;
    }

    @Override
    protected MessageHandler<PaymentSucceededMessage> getMessageHandler() {
        return message -> {
            PaymentSucceededMessage payload = message.getPayload();
            LOGGER.info("收到支付成功消息，开始扣减库存，orderNo={}", payload.orderNo());
            productService.deductStockForPaidOrder(payload);
        };
    }

    @Override
    protected Class<PaymentSucceededMessage> getPayloadClass() {
        return PaymentSucceededMessage.class;
    }
}
