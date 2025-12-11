package com.basebackend.generator.cache;

import com.basebackend.generator.core.metadata.TableMetadata;
import com.basebackend.generator.entity.GenTemplate;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 代码生成器缓存管理服务
 * 
 * 提供模板缓存和元数据缓存功能，减少数据库访问和元数据读取开销
 * 使用Caffeine作为本地缓存实现
 */
@Slf4j
@Component
public class GeneratorCacheManager {

    /**
     * 模板缓存
     * Key: templateGroupId
     * Value: 模板列表
     */
    private final Cache<Long, List<GenTemplate>> templateCache;

    /**
     * 表元数据缓存
     * Key: "datasourceId:tableName"
     * Value: 表元数据
     */
    private final Cache<String, TableMetadata> metadataCache;

    /**
     * 模板内容渲染缓存
     * Key: "templateId:dataModelHash"
     * Value: 渲染后的内容
     */
    private final Cache<String, String> renderCache;

    public GeneratorCacheManager() {
        // 模板缓存配置：最大100个分组，30分钟过期
        this.templateCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build();

        // 元数据缓存配置：最大500个表，10分钟过期
        this.metadataCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build();

        // 渲染缓存配置：最大1000条，5分钟过期
        this.renderCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();

        log.info("代码生成器缓存管理器初始化完成");
    }

    // ==================== 模板缓存操作 ====================

    /**
     * 获取缓存的模板列表
     *
     * @param templateGroupId 模板分组ID
     * @return 缓存的模板列表，不存在返回null
     */
    public List<GenTemplate> getTemplates(Long templateGroupId) {
        return templateCache.getIfPresent(templateGroupId);
    }

    /**
     * 缓存模板列表
     *
     * @param templateGroupId 模板分组ID
     * @param templates       模板列表
     */
    public void putTemplates(Long templateGroupId, List<GenTemplate> templates) {
        templateCache.put(templateGroupId, templates);
        log.debug("缓存模板列表: groupId={}, count={}", templateGroupId, templates.size());
    }

    /**
     * 使指定模板分组的缓存失效
     *
     * @param templateGroupId 模板分组ID
     */
    public void invalidateTemplates(Long templateGroupId) {
        templateCache.invalidate(templateGroupId);
        log.debug("使模板缓存失效: groupId={}", templateGroupId);
    }

    /**
     * 使所有模板缓存失效
     */
    public void invalidateAllTemplates() {
        templateCache.invalidateAll();
        log.info("使所有模板缓存失效");
    }

    // ==================== 元数据缓存操作 ====================

    /**
     * 构建元数据缓存Key
     */
    private String buildMetadataKey(Long datasourceId, String tableName) {
        return datasourceId + ":" + tableName;
    }

    /**
     * 获取缓存的表元数据
     *
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @return 缓存的表元数据，不存在返回null
     */
    public TableMetadata getMetadata(Long datasourceId, String tableName) {
        String key = buildMetadataKey(datasourceId, tableName);
        return metadataCache.getIfPresent(key);
    }

    /**
     * 缓存表元数据
     *
     * @param datasourceId 数据源ID
     * @param tableName    表名
     * @param metadata     表元数据
     */
    public void putMetadata(Long datasourceId, String tableName, TableMetadata metadata) {
        String key = buildMetadataKey(datasourceId, tableName);
        metadataCache.put(key, metadata);
        log.debug("缓存表元数据: datasource={}, table={}", datasourceId, tableName);
    }

    /**
     * 使指定表的元数据缓存失效
     *
     * @param datasourceId 数据源ID
     * @param tableName    表名
     */
    public void invalidateMetadata(Long datasourceId, String tableName) {
        String key = buildMetadataKey(datasourceId, tableName);
        metadataCache.invalidate(key);
        log.debug("使元数据缓存失效: datasource={}, table={}", datasourceId, tableName);
    }

    /**
     * 使指定数据源的所有元数据缓存失效
     *
     * @param datasourceId 数据源ID
     */
    public void invalidateMetadataByDatasource(Long datasourceId) {
        String prefix = datasourceId + ":";
        metadataCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        log.debug("使数据源所有元数据缓存失效: datasource={}", datasourceId);
    }

    /**
     * 使所有元数据缓存失效
     */
    public void invalidateAllMetadata() {
        metadataCache.invalidateAll();
        log.info("使所有元数据缓存失效");
    }

    // ==================== 渲染缓存操作 ====================

    /**
     * 构建渲染缓存Key
     */
    private String buildRenderKey(Long templateId, int dataModelHash) {
        return templateId + ":" + dataModelHash;
    }

    /**
     * 获取缓存的渲染结果
     *
     * @param templateId    模板ID
     * @param dataModelHash 数据模型哈希值
     * @return 缓存的渲染结果，不存在返回null
     */
    public String getRenderResult(Long templateId, int dataModelHash) {
        String key = buildRenderKey(templateId, dataModelHash);
        return renderCache.getIfPresent(key);
    }

    /**
     * 缓存渲染结果
     *
     * @param templateId    模板ID
     * @param dataModelHash 数据模型哈希值
     * @param content       渲染后的内容
     */
    public void putRenderResult(Long templateId, int dataModelHash, String content) {
        String key = buildRenderKey(templateId, dataModelHash);
        renderCache.put(key, content);
        log.debug("缓存渲染结果: templateId={}", templateId);
    }

    /**
     * 使指定模板的渲染缓存失效
     *
     * @param templateId 模板ID
     */
    public void invalidateRenderByTemplate(Long templateId) {
        String prefix = templateId + ":";
        renderCache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
        log.debug("使模板渲染缓存失效: templateId={}", templateId);
    }

    /**
     * 使所有渲染缓存失效
     */
    public void invalidateAllRender() {
        renderCache.invalidateAll();
        log.info("使所有渲染缓存失效");
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计摘要
     */
    public CacheStats getStats() {
        return new CacheStats(
                templateCache.estimatedSize(),
                templateCache.stats().hitRate(),
                metadataCache.estimatedSize(),
                metadataCache.stats().hitRate(),
                renderCache.estimatedSize(),
                renderCache.stats().hitRate());
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        invalidateAllTemplates();
        invalidateAllMetadata();
        invalidateAllRender();
        log.info("所有缓存已清空");
    }

    /**
     * 缓存统计信息
     */
    public record CacheStats(
            long templateCacheSize,
            double templateCacheHitRate,
            long metadataCacheSize,
            double metadataCacheHitRate,
            long renderCacheSize,
            double renderCacheHitRate) {
        @Override
        public String toString() {
            return String.format(
                    "CacheStats{template=[size=%d, hitRate=%.2f%%], metadata=[size=%d, hitRate=%.2f%%], render=[size=%d, hitRate=%.2f%%]}",
                    templateCacheSize, templateCacheHitRate * 100,
                    metadataCacheSize, metadataCacheHitRate * 100,
                    renderCacheSize, renderCacheHitRate * 100);
        }
    }
}
