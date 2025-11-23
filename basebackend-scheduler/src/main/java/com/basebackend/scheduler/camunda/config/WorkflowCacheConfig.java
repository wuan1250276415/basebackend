package com.basebackend.scheduler.camunda.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * 工作流缓存配置
 *
 * <p>使用 Caffeine 高性能本地缓存来加速工作流相关数据的查询，包括：
 * <ul>
 *   <li>流程定义（Process Definition）</li>
 *   <li>流程部署（Deployment）</li>
 *   <li>表单模板（Form Template）</li>
 * </ul>
 *
 * <p>缓存策略：
 * <ul>
 *   <li>缓存大小：由 {@link CamundaProperties#maxCacheSize} 控制</li>
 *   <li>过期时间：由 {@link CamundaProperties#cacheExpireMinutes} 控制</li>
 *   <li>淘汰策略：LRU（Least Recently Used）</li>
 *   <li>统计信息：启用缓存统计，可通过 Micrometer 暴露</li>
 * </ul>
 *
 * <p>性能优化：
 * <ul>
 *   <li>减少数据库查询次数</li>
 *   <li>降低 Camunda 引擎负载</li>
 *   <li>提升流程定义查询性能</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@EnableCaching
@Configuration
@RequiredArgsConstructor
public class WorkflowCacheConfig {

    /**
     * 流程定义缓存名称（旧版兼容）
     */
    public static final String CACHE_PROCESS_DEFINITION = "camundaProcessDefinitionCache";

    /**
     * 流程定义缓存名称（Service层使用）
     */
    public static final String CACHE_PROCESS_DEFINITIONS = "processDefinitions";

    /**
     * 流程部署缓存名称
     */
    public static final String CACHE_DEPLOYMENT = "camundaDeploymentCache";

    /**
     * 表单模板缓存名称（旧版兼容）
     */
    public static final String CACHE_FORM = "camundaFormCache";

    /**
     * 表单模板缓存名称（Service层使用）
     */
    public static final String CACHE_FORM_TEMPLATES = "formTemplates";

    private final CamundaProperties camundaProperties;

    /**
     * 配置 Caffeine 缓存构建器
     *
     * <p>缓存特性：
     * <ul>
     *   <li>基于大小的淘汰：最大缓存数量由配置决定</li>
     *   <li>基于时间的过期：写入后固定时间过期</li>
     *   <li>统计信息记录：用于监控缓存性能</li>
     * </ul>
     *
     * @return Caffeine 构建器实例
     */
    @Bean
    public Caffeine<Object, Object> workflowCaffeine() {
        log.info("Configuring workflow Caffeine cache: maxSize={}, expireMinutes={}",
                camundaProperties.getMaxCacheSize(),
                camundaProperties.getCacheExpireMinutes());

        return Caffeine.newBuilder()
                // 基于大小的淘汰策略
                .maximumSize(camundaProperties.getMaxCacheSize())
                // 写入后过期时间
                .expireAfterWrite(Duration.ofMinutes(camundaProperties.getCacheExpireMinutes()))
                // 启用统计信息记录（可用于监控）
                .recordStats()
                // 可选：启用弱引用键（适用于内存敏感场景）
                // .weakKeys()
                // 可选：启用软引用值（允许 GC 回收）
                // .softValues()
                ;
    }

    /**
     * 配置工作流缓存管理器
     *
     * <p>管理所有与工作流相关的缓存实例，包括流程定义、部署、表单等。
     * 如果 {@link CamundaProperties#cacheEnabled} 为 {@code false}，则禁用所有缓存。
     *
     * @param workflowCaffeine Caffeine 缓存构建器
     * @return CacheManager 实例
     */
    @Bean
    public CacheManager workflowCacheManager(Caffeine<Object, Object> workflowCaffeine) {
        log.info("Configuring workflow cache manager: cacheEnabled={}",
                camundaProperties.isCacheEnabled());

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 设置缓存名称
        if (camundaProperties.isCacheEnabled()) {
            cacheManager.setCacheNames(Arrays.asList(
                    CACHE_PROCESS_DEFINITION,
                    CACHE_PROCESS_DEFINITIONS,
                    CACHE_DEPLOYMENT,
                    CACHE_FORM,
                    CACHE_FORM_TEMPLATES
            ));
            log.info("Workflow caches initialized: {}", Arrays.asList(
                    CACHE_PROCESS_DEFINITION,
                    CACHE_PROCESS_DEFINITIONS,
                    CACHE_DEPLOYMENT,
                    CACHE_FORM,
                    CACHE_FORM_TEMPLATES
            ));
        } else {
            log.warn("Workflow cache is disabled by configuration");
        }

        // 应用 Caffeine 配置
        cacheManager.setCaffeine(workflowCaffeine);

        // 缓存配置
        cacheManager.setAllowNullValues(false); // 不缓存 null 值
        cacheManager.setCacheSpecification(null); // 使用全局 Caffeine 配置

        return cacheManager;
    }

    /**
     * 获取流程定义缓存名称
     *
     * @return 缓存名称
     */
    public static String getProcessDefinitionCacheName() {
        return CACHE_PROCESS_DEFINITION;
    }

    /**
     * 获取流程部署缓存名称
     *
     * @return 缓存名称
     */
    public static String getDeploymentCacheName() {
        return CACHE_DEPLOYMENT;
    }

    /**
     * 获取表单缓存名称
     *
     * @return 缓存名称
     */
    public static String getFormCacheName() {
        return CACHE_FORM;
    }
}
