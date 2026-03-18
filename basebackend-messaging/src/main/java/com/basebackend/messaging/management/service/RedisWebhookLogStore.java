package com.basebackend.messaging.management.service;

import com.basebackend.common.dto.PageResult;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.messaging.webhook.WebhookLog;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RedisWebhookLogStore implements WebhookLogStore {

    private static final String LOG_ID_KEY = "msg:webhook:log:id";
    private static final String LOG_INDEX_KEY = "msg:webhook:log:index";
    private static final String LOG_DATA_KEY_PREFIX = "msg:webhook:log:data:";
    private static final Duration LOG_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public RedisWebhookLogStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public WebhookLog save(WebhookLog webhookLog) {
        LocalDateTime now = LocalDateTime.now();
        if (webhookLog.getCallTime() == null) {
            webhookLog.setCallTime(now);
        }
        if (webhookLog.getCreateTime() == null) {
            webhookLog.setCreateTime(now);
        }

        Long id = redisTemplate.opsForValue().increment(LOG_ID_KEY);
        if (id == null) {
            throw new IllegalStateException("无法生成Webhook日志ID");
        }

        webhookLog.setId(id);
        redisTemplate.opsForValue().set(buildDataKey(id), JsonUtils.toJsonString(webhookLog), LOG_TTL);
        redisTemplate.opsForZSet().add(LOG_INDEX_KEY, String.valueOf(id), toEpochMilli(webhookLog.getCallTime()));
        trimExpiredIndex();
        return webhookLog;
    }

    @Override
    public WebhookLog findById(Long id) {
        if (id == null) {
            return null;
        }
        String raw = redisTemplate.opsForValue().get(buildDataKey(id));
        if (raw == null) {
            return null;
        }
        return JsonUtils.parseObject(raw, WebhookLog.class);
    }

    @Override
    public PageResult<WebhookLog> page(long current, long size,
                                       Long webhookId,
                                       String eventType,
                                       Boolean success,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime) {
        Set<String> ids = findCandidateIds(startTime, endTime);
        if (ids == null || ids.isEmpty()) {
            return PageResult.empty(current, size);
        }

        List<WebhookLog> filtered = ids.stream()
                .map(id -> {
                    try {
                        return findById(Long.valueOf(id));
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(log -> webhookId == null || webhookId.equals(log.getWebhookId()))
                .filter(log -> eventType == null || eventType.isBlank() || eventType.equalsIgnoreCase(log.getEventType()))
                .filter(log -> success == null || success.equals(log.getSuccess()))
                .sorted(Comparator.comparing(WebhookLog::getCallTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = (int) Math.max((current - 1) * size, 0);
        if (fromIndex >= filtered.size()) {
            return PageResult.of(List.of(), (long) filtered.size(), current, size);
        }

        int toIndex = (int) Math.min(fromIndex + size, filtered.size());
        return PageResult.of(new ArrayList<>(filtered.subList(fromIndex, toIndex)),
                (long) filtered.size(), current, size);
    }

    private Set<String> findCandidateIds(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null || endTime != null) {
            double minScore = startTime != null ? toEpochMilli(startTime) : Double.NEGATIVE_INFINITY;
            double maxScore = endTime != null ? toEpochMilli(endTime) : Double.POSITIVE_INFINITY;
            return redisTemplate.opsForZSet().reverseRangeByScore(LOG_INDEX_KEY, minScore, maxScore);
        }
        return redisTemplate.opsForZSet().reverseRange(LOG_INDEX_KEY, 0, -1);
    }

    private void trimExpiredIndex() {
        double expiredBefore = toEpochMilli(LocalDateTime.now().minus(LOG_TTL));
        redisTemplate.opsForZSet().removeRangeByScore(LOG_INDEX_KEY, Double.NEGATIVE_INFINITY, expiredBefore);
    }

    private String buildDataKey(Long id) {
        return LOG_DATA_KEY_PREFIX + id;
    }

    private double toEpochMilli(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
