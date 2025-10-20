package com.basebackend.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置
 */
@Configuration
@ConditionalOnProperty(prefix = "messaging.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMQConfig {

    private final MessagingProperties properties;

    public RabbitMQConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // 启用发送确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 消息未成功到达Exchange
                System.err.println("Message send failed: " + cause);
            }
        });

        // 启用返回确认（消息未路由到队列时触发）
        template.setReturnsCallback(returned -> {
            System.err.println("Message returned: " + returned.getMessage());
        });

        template.setMandatory(true);

        return template;
    }

    /**
     * 重试模板
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        // 重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(properties.getRetry().getMaxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);

        // 指数退避策略
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.getRetry().getInitialInterval());
        backOffPolicy.setMultiplier(properties.getRetry().getMultiplier());
        backOffPolicy.setMaxInterval(properties.getRetry().getMaxInterval());
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return retryTemplate;
    }

    /**
     * 监听容器工厂（支持重试和死信）
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(1);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    /**
     * 默认交换机
     */
    @Bean
    public DirectExchange defaultExchange() {
        return new DirectExchange(properties.getRabbitmq().getDefaultExchange(), true, false);
    }

    /**
     * 延迟交换机（需要安装rabbitmq_delayed_message_exchange插件）
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging.rabbitmq", name = "delay-plugin-enabled", havingValue = "true")
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(
                properties.getRabbitmq().getDelayExchange(),
                "x-delayed-message",
                true,
                false,
                args
        );
    }

    /**
     * 死信交换机
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(properties.getDeadLetter().getExchange(), true, false);
    }

    /**
     * 死信队列
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(properties.getDeadLetter().getQueue()).build();
    }

    /**
     * 死信队列绑定
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging.dead-letter", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.getDeadLetter().getRoutingKey());
    }
}
