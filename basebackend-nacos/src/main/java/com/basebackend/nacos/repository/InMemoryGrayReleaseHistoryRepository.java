/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.basebackend.nacos.repository;

import com.basebackend.nacos.model.GrayReleaseHistory;
import com.basebackend.nacos.repository.GrayReleaseHistoryRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryGrayReleaseHistoryRepository
implements GrayReleaseHistoryRepository {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(InMemoryGrayReleaseHistoryRepository.class);
    private final Map<Long, GrayReleaseHistory> storage = new ConcurrentHashMap<Long, GrayReleaseHistory>();
    private final AtomicLong idGenerator = new AtomicLong(1L);
    private static final int MAX_RECORDS = 10000;

    @Override
    public GrayReleaseHistory save(GrayReleaseHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("\u5386\u53f2\u8bb0\u5f55\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (this.storage.size() >= 10000) {
            this.cleanupOldRecords();
        }
        if (history.getId() == null) {
            history.setId(this.idGenerator.getAndIncrement());
        }
        if (history.getCreateTime() == null) {
            history.setCreateTime(LocalDateTime.now());
        }
        this.storage.put(history.getId(), history);
        log.debug("\u4fdd\u5b58\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u8bb0\u5f55: id={}, dataId={}", (Object)history.getId(), (Object)history.getDataId());
        return history;
    }

    @Override
    public Optional<GrayReleaseHistory> findById(Long id) {
        return Optional.ofNullable(this.storage.get(id));
    }

    @Override
    public List<GrayReleaseHistory> findByDataId(String dataId) {
        if (dataId == null || dataId.isEmpty()) {
            return Collections.emptyList();
        }
        return this.storage.values().stream().filter(h -> dataId.equals(h.getDataId())).sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed()).collect(Collectors.toList());
    }

    @Override
    public Optional<GrayReleaseHistory> findLatestByDataId(String dataId) {
        if (dataId == null || dataId.isEmpty()) {
            return Optional.empty();
        }
        return this.storage.values().stream().filter(h -> dataId.equals(h.getDataId())).max(Comparator.comparing(GrayReleaseHistory::getOperationTime));
    }

    @Override
    public List<GrayReleaseHistory> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return this.storage.values().stream().filter(h -> {
            LocalDateTime opTime = h.getOperationTime();
            if (opTime == null) {
                return false;
            }
            boolean afterStart = startTime == null || !opTime.isBefore(startTime);
            boolean beforeEnd = endTime == null || !opTime.isAfter(endTime);
            return afterStart && beforeEnd;
        }).sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<GrayReleaseHistory> findByOperationType(String operationType) {
        if (operationType == null || operationType.isEmpty()) {
            return Collections.emptyList();
        }
        return this.storage.values().stream().filter(h -> operationType.equals(h.getOperationType())).sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<GrayReleaseHistory> findAll() {
        return this.storage.values().stream().sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime).reversed()).collect(Collectors.toList());
    }

    @Override
    public int deleteBeforeTime(LocalDateTime beforeTime) {
        if (beforeTime == null) {
            return 0;
        }
        List<Long> toDelete = this.storage.values().stream().filter(h -> h.getOperationTime() != null && h.getOperationTime().isBefore(beforeTime)).map(GrayReleaseHistory::getId).collect(Collectors.toList());
        toDelete.forEach(this.storage::remove);
        log.info("\u6e05\u7406 {} \u4e4b\u524d\u7684\u7070\u5ea6\u53d1\u5e03\u5386\u53f2\u8bb0\u5f55\uff0c\u5171\u5220\u9664 {} \u6761", (Object)beforeTime, (Object)toDelete.size());
        return toDelete.size();
    }

    @Override
    public long count() {
        return this.storage.size();
    }

    private void cleanupOldRecords() {
        List sortedIds = this.storage.values().stream().sorted(Comparator.comparing(GrayReleaseHistory::getOperationTime)).map(GrayReleaseHistory::getId).collect(Collectors.toList());
        int deleteCount = sortedIds.size() / 2;
        for (int i = 0; i < deleteCount; ++i) {
            this.storage.remove(sortedIds.get(i));
        }
        log.info("\u5185\u5b58\u5b58\u50a8\u8fbe\u5230\u4e0a\u9650\uff0c\u6e05\u7406\u65e7\u8bb0\u5f55 {} \u6761", (Object)deleteCount);
    }
}

