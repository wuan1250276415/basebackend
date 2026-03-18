package com.basebackend.messaging.management.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.basebackend.messaging.entity.DeadLetterEntity;
import com.basebackend.messaging.entity.MessageLogEntity;
import com.basebackend.messaging.mapper.DeadLetterMapper;
import com.basebackend.messaging.mapper.MessageLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("消息监控服务测试")
class MessagingMonitorServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, MessageLogEntity.class);
        TableInfoHelper.initTableInfo(assistant, DeadLetterEntity.class);
    }

    @Mock
    private MessageLogMapper messageLogMapper;

    @Mock
    private DeadLetterMapper deadLetterMapper;

    @Mock
    private ListableBeanFactory beanFactory;

    private MessagingMonitorService messagingMonitorService;

    @BeforeEach
    void setUp() {
        messagingMonitorService = new MessagingMonitorService(messageLogMapper, deadLetterMapper, beanFactory);
    }

    @Test
    @DisplayName("统计接口返回消息总览和成功率")
    void getStatisticsShouldReturnOverview() {
        when(messageLogMapper.selectCount(any()))
                .thenReturn(100L, 10L, 20L, 60L, 10L);
        when(deadLetterMapper.selectCount(any())).thenReturn(5L);

        var statistics = messagingMonitorService.getStatistics();

        assertEquals(100L, statistics.total());
        assertEquals(10L, statistics.pending());
        assertEquals(20L, statistics.sent());
        assertEquals(60L, statistics.consumed());
        assertEquals(10L, statistics.failed());
        assertEquals(5L, statistics.deadLetter());
        assertEquals("60.00%", statistics.successRate());
    }

    @Test
    @DisplayName("队列监控接口返回吞吐与消费者数量")
    void getQueueMonitorShouldReturnQueueMetrics() {
        when(messageLogMapper.selectCount(any()))
                .thenReturn(100L, 5L, 7L, 60L, 30L);
        when(messageLogMapper.selectObjs(any())).thenReturn(List.of("topic-a", "topic-b"));
        when(beanFactory.getBeansOfType(RocketMQListener.class))
                .thenReturn(Map.of("consumerA", new RocketMQListener<String>() {
                    @Override
                    public void onMessage(String message) {
                    }
                }));

        var queueMonitor = messagingMonitorService.getQueueMonitor();

        assertEquals(2L, queueMonitor.queueCount());
        assertEquals(100L, queueMonitor.totalMessages());
        assertEquals(5L, queueMonitor.readyMessages());
        assertEquals(7L, queueMonitor.unackedMessages());
        assertEquals(1L, queueMonitor.consumerCount());
        assertEquals(1.0D, queueMonitor.messageRate());
        assertEquals(0.5D, queueMonitor.ackRate());
    }
}
