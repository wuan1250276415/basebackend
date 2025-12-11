package com.basebackend.nacos.repository;

import com.basebackend.nacos.model.GrayReleaseHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 灰度发布历史仓储接口
 * <p>
 * 定义灰度发布历史的持久化操作，支持多种实现方式（内存、数据库等）
 * </p>
 */
public interface GrayReleaseHistoryRepository {

    /**
     * 保存灰度发布历史记录
     *
     * @param history 历史记录
     * @return 保存后的历史记录（包含生成的ID）
     */
    GrayReleaseHistory save(GrayReleaseHistory history);

    /**
     * 根据ID查询历史记录
     *
     * @param id 历史记录ID
     * @return 历史记录
     */
    Optional<GrayReleaseHistory> findById(Long id);

    /**
     * 根据Data ID查询历史记录列表
     *
     * @param dataId 配置Data ID
     * @return 历史记录列表
     */
    List<GrayReleaseHistory> findByDataId(String dataId);

    /**
     * 根据Data ID查询最新的历史记录
     *
     * @param dataId 配置Data ID
     * @return 最新的历史记录
     */
    Optional<GrayReleaseHistory> findLatestByDataId(String dataId);

    /**
     * 根据时间范围查询历史记录
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 历史记录列表
     */
    List<GrayReleaseHistory> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据操作类型查询历史记录
     *
     * @param operationType 操作类型
     * @return 历史记录列表
     */
    List<GrayReleaseHistory> findByOperationType(String operationType);

    /**
     * 查询所有历史记录
     *
     * @return 所有历史记录
     */
    List<GrayReleaseHistory> findAll();

    /**
     * 删除指定时间之前的历史记录
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    int deleteBeforeTime(LocalDateTime beforeTime);

    /**
     * 统计历史记录总数
     *
     * @return 记录总数
     */
    long count();
}
