package com.basebackend.message.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 示例消息消费者
 * 使用时需要指定具体的topic和consumerGroup
 */
@Slf4j
//@Component
//@RocketMQMessageListener(
//        topic = "example-topic",
//        consumerGroup = "example-consumer-group"
//)
public class ExampleMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("接收到消息: {}", message);
        try {
            // 处理消息逻辑
            processMessage(message);
            log.info("消息处理成功: {}", message);
        } catch (Exception e) {
            log.error("消息处理失败: {}", message, e);
            // 可以根据业务需求决定是否抛出异常触发重试
            throw new RuntimeException("消息处理失败", e);
        }
    }

    /**
     * 处理消息的业务逻辑
     */
    private void processMessage(String message) {
        // 实现具体的业务逻辑
        log.debug("处理消息: {}", message);
    }
}
