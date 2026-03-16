/*
 * Decompiled with CFR 0.152.
 */
package com.basebackend.nacos.repository;

import com.basebackend.nacos.model.GrayReleaseHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GrayReleaseHistoryRepository {
    public GrayReleaseHistory save(GrayReleaseHistory var1);

    public Optional<GrayReleaseHistory> findById(Long var1);

    public List<GrayReleaseHistory> findByDataId(String var1);

    public Optional<GrayReleaseHistory> findLatestByDataId(String var1);

    public List<GrayReleaseHistory> findByTimeRange(LocalDateTime var1, LocalDateTime var2);

    public List<GrayReleaseHistory> findByOperationType(String var1);

    public List<GrayReleaseHistory> findAll();

    public int deleteBeforeTime(LocalDateTime var1);

    public long count();
}

