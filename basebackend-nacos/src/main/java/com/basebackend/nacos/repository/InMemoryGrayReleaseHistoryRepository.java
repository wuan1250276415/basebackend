package com.basebackend.nacos.repository;

import com.basebackend.nacos.model.GrayReleaseHistory;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 内存实现的灰度发布历史仓储
 * <p>
 * 默认实现，适用于开发测试环境。生产环境建议实现数据库版本。
 * 通过 NacosAutoConfiguration 自动注册，当没有其他实现时启用。
 * </p>
 */
@Slf4j
public class InMemoryGrayReleaseHistoryRepository implements GrayReleaseHistoryRepository {

    /**
     * 内存存储，使用 ConcurrentHashMap 保证线程安全
     */
    private final Map<Long, GrayReleaseHistory> storage = new ConcurrentHashMap<>();

    /**
     * ID 生成器
     */
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 最大存储记录数（防止内存溢出）
     */
    private static final int MAX_RECORDS = 10000;

    @Override
    public GrayReleaseHistory save(GrayReleaseHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("历史记录不能为空");
        }

        // 检查存储容量，超出时清理旧记录
        if (storage.size() >= MAX_RECORDS) {
            cleanupOldRecords();
        }

        // 生成ID
        if (history.getId() == null) {
            history.setId(idGenerator.getAndIncrement());
        }

        // 设置创建时间
        if (history.getCreateTime() == null) {
            history.setCreateTime(LocalDateTime.now());
        }

        storage.put(history.getId(), history);
        log.debug("保存灰度发布历史记录: id={}, dataId={}", history.getId(), history.getDataId());

        return history;
    }

    @Override
    public Optional<GrayReleaseHistory> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<GrayReleaseHistory> findByDataId(String dataId) {
        if (dataId == null || dataId.isEmpty()) {
            return Collections.emptyList();
        }

        return storage.values().stream()
                .filter(h -> dataId.equals(h.getDataId()))
                .sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GrayReleaseHistory> findLatestByDataId(String dataId) {
        if (dataId == null || dataId.isEmpty()) {
            return Optional.empty();
        }

        return storage.values().stream()
                .filter(h -> dataId.equals(h.getDataId()))
                .max(Comparator.comparing(GrayReleaseHistory::getOperationTime));
    }

    @Override
    public List<GrayReleaseHistory> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return storage.values().stream()
                .filter(h -> {
                    LocalDateTime opTime = h.getOperationTime();
                    if (opTime == null) {
                        return false;
                    }
                    boolean afterStart = startTime == null || !opTime.isBefore(startTime);
                    boolean beforeEnd = endTime == null || !opTime.isAfter(endTime);
                    return afterStart && beforeEnd;
                })
                .sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<GrayReleaseHistory> findByOperationType(String operationType) {
        if (operationType == null || operationType.isEmpty()) {
            return Collections.emptyList();
        }

        return storage.values().stream()
                .filter(h -> operationType.equals(h.getOperationType()))
                .sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<GrayReleaseHistory> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public int deleteBeforeTime(LocalDateTime beforeTime) {
        if (beforeTime == null) {
            return 0;
        }

        List<Long> toDelete = storage.values().stream()
                .filter(h -> h.getOperationTime() != null && h.getOperationTime().isBefore(beforeTime))
                .map(GrayReleaseHistory::getId)
                .collect(Collectors.toList());

        toDelete.forEach(storage::remove);
        log.info("清理 {} 之前的灰度发布历史记录，共删除 {} 条", beforeTime, toDelete.size());

        return toDelete.size();
    }

    @Override
    public long count() {
        return storage.size();
    }

    /**
     * 清理旧记录（保留最近的一半记录）
     */
    private void cleanupOldRecords() {
        List<Long> sortedIds = storage.values().stream()
                .sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime))
                .map(GrayReleaseHistory::getId)
                .collect(Collectors.toList());

        // 删除前一半的记录
        int deleteCount = sortedIds.size() / 2;
        for (int i = 0; i < deleteCount; i++) {
            storage.remove(sortedIds.get(i));
        }

        log.info("内存存储达到上限，清理旧记录 {} 条", deleteCount);
    }
}
