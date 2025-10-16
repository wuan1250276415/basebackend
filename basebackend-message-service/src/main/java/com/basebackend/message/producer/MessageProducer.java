package com.basebackend.message.producer;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送同步消息
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendSyncMessage(String topic, Object message) {
        try {
            String jsonMessage = JSON.toJSONString(message);
            rocketMQTemplate.syncSend(topic, jsonMessage);
            log.info("发送同步消息成功，Topic: {}, Message: {}", topic, jsonMessage);
        } catch (Exception e) {
            log.error("发送同步消息失败，Topic: {}, Message: {}", topic, message, e);
            throw new RuntimeException("发送消息失败", e);
        }
    }

    /**
     * 发送异步消息
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendAsyncMessage(String topic, Object message) {
        try {
            String jsonMessage = JSON.toJSONString(message);
            rocketMQTemplate.asyncSend(topic, jsonMessage, new org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener() {
                @Override
                public org.apache.rocketmq.spring.support.RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                    log.info("发送异步消息成功，Topic: {}, Message: {}", topic, jsonMessage);
                    return org.apache.rocketmq.spring.support.RocketMQLocalTransactionState.COMMIT;
                }

                @Override
                public org.apache.rocketmq.spring.support.RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
                    return org.apache.rocketmq.spring.support.RocketMQLocalTransactionState.COMMIT;
                }
            });
        } catch (Exception e) {
            log.error("发送异步消息失败，Topic: {}, Message: {}", topic, message, e);
        }
    }

    /**
     * 发送单向消息（不关心发送结果）
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendOneWayMessage(String topic, Object message) {
        try {
            String jsonMessage = JSON.toJSONString(message);
            rocketMQTemplate.sendOneWay(topic, jsonMessage);
            log.info("发送单向消息，Topic: {}, Message: {}", topic, jsonMessage);
        } catch (Exception e) {
            log.error("发送单向消息失败，Topic: {}, Message: {}", topic, message, e);
        }
    }

    /**
     * 发送延迟消息
     *
     * @param topic 主题
     * @param message 消息内容
     * @param delayLevel 延迟级别（1-18）
     */
    public void sendDelayMessage(String topic, Object message, int delayLevel) {
        try {
            String jsonMessage = JSON.toJSONString(message);
            Message<String> msg = MessageBuilder.withPayload(jsonMessage).build();
            rocketMQTemplate.syncSend(topic, msg, 3000, delayLevel);
            log.info("发送延迟消息成功，Topic: {}, Message: {}, DelayLevel: {}", topic, jsonMessage, delayLevel);
        } catch (Exception e) {
            log.error("发送延迟消息失败，Topic: {}, Message: {}", topic, message, e);
            throw new RuntimeException("发送延迟消息失败", e);
        }
    }

    /**
     * 发送带标签的消息
     *
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    public void sendMessageWithTag(String topic, String tag, Object message) {
        try {
            String destination = topic + ":" + tag;
            String jsonMessage = JSON.toJSONString(message);
            rocketMQTemplate.syncSend(destination, jsonMessage);
            log.info("发送带标签消息成功，Topic: {}, Tag: {}, Message: {}", topic, tag, jsonMessage);
        } catch (Exception e) {
            log.error("发送带标签消息失败，Topic: {}, Tag: {}, Message: {}", topic, tag, message, e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
