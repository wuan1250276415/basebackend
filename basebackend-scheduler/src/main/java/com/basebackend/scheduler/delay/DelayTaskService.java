package com.basebackend.scheduler.delay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 延迟任务服务
 * 支持订单超时、消息延迟发送、数据清理、业务流转等场景
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayTaskService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String DELAY_TASK_PREFIX = "delay:task:";

    /**
     * 提交延迟任务
     *
     * @param taskType  任务类型
     * @param taskId    任务ID
     * @param params    任务参数
     * @param delay     延迟时间
     * @return 任务键
     */
    public String submitDelayTask(DelayTaskType taskType, String taskId,
                                   Map<String, Object> params, Duration delay) {
        String taskKey = DELAY_TASK_PREFIX + taskType.name() + ":" + taskId;

        // 将任务信息存储到Redis，设置过期时间
        DelayTask task = DelayTask.builder()
                .taskId(taskId)
                .taskType(taskType)
                .params(params)
                .createTime(System.currentTimeMillis())
                .executeTime(System.currentTimeMillis() + delay.toMillis())
                .build();

        redisTemplate.opsForValue().set(taskKey, task, delay.toMillis(), TimeUnit.MILLISECONDS);

        log.info("提交延迟任务成功 - 类型: {}, ID: {}, 延迟: {}ms",
                taskType, taskId, delay.toMillis());

        return taskKey;
    }

    /**
     * 取消延迟任务
     *
     * @param taskKey 任务键
     */
    public void cancelDelayTask(String taskKey) {
        redisTemplate.delete(taskKey);
        log.info("取消延迟任务: {}", taskKey);
    }

    /**
     * 延迟任务数据模型
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DelayTask implements java.io.Serializable {
        private String taskId;
        private DelayTaskType taskType;
        private Map<String, Object> params;
        private Long createTime;
        private Long executeTime;
    }
}
