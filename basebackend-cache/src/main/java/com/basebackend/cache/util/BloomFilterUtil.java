package com.basebackend.cache.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 布隆过滤器工具类
 * 
 * 用于防止缓存穿透：
 * - 快速判断某个键是否可能存在
 * - 如果返回 false，则键一定不存在
 * - 如果返回 true，则键可能存在（存在误判率）
 * 
 * 特性：
 * - 支持多个命名空间的布隆过滤器
 * - 可配置预期元素数量和误判率
 * - 线程安全
 */
@Slf4j
@Component
public class BloomFilterUtil {

    /**
     * 布隆过滤器映射表（支持多个命名空间）
     */
    private final ConcurrentHashMap<String, BloomFilter<String>> bloomFilters = new ConcurrentHashMap<>();
    
    /**
     * 默认命名空间
     */
    private static final String DEFAULT_NAMESPACE = "default";
    
    /**
     * 默认预期元素数量
     */
    private static final int DEFAULT_EXPECTED_INSERTIONS = 1_000_000;
    
    /**
     * 默认误判率（1%）
     */
    private static final double DEFAULT_FALSE_POSITIVE_PROBABILITY = 0.01;

    /**
     * 初始化默认布隆过滤器
     */
    @PostConstruct
    public void init() {
        log.info("Initializing BloomFilterUtil");
        createBloomFilter(DEFAULT_NAMESPACE, DEFAULT_EXPECTED_INSERTIONS, DEFAULT_FALSE_POSITIVE_PROBABILITY);
        log.info("BloomFilterUtil initialized with default namespace");
    }

    /**
     * 创建布隆过滤器
     * 
     * @param namespace 命名空间
     * @param expectedInsertions 预期元素数量
     * @param falsePositiveProbability 误判率
     */
    public void createBloomFilter(String namespace, int expectedInsertions, double falsePositiveProbability) {
        if (namespace == null || namespace.trim().isEmpty()) {
            namespace = DEFAULT_NAMESPACE;
        }
        
        BloomFilter<String> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions,
                falsePositiveProbability
        );
        
        bloomFilters.put(namespace, bloomFilter);
        
        log.info("Created bloom filter for namespace: {}, expectedInsertions: {}, falsePositiveProbability: {}",
                namespace, expectedInsertions, falsePositiveProbability);
    }

    /**
     * 添加元素到默认布隆过滤器
     * 
     * @param key 键
     */
    public void add(String key) {
        add(DEFAULT_NAMESPACE, key);
    }

    /**
     * 添加元素到指定命名空间的布隆过滤器
     * 
     * @param namespace 命名空间
     * @param key 键
     */
    public void add(String namespace, String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Attempted to add null or empty key to bloom filter");
            return;
        }
        
        BloomFilter<String> bloomFilter = getOrCreateBloomFilter(namespace);
        bloomFilter.put(key);
        
        log.debug("Added key to bloom filter: namespace={}, key={}", namespace, key);
    }

    /**
     * 检查元素是否可能存在于默认布隆过滤器
     * 
     * @param key 键
     * @return true 如果可能存在，false 如果一定不存在
     */
    public boolean mightContain(String key) {
        return mightContain(DEFAULT_NAMESPACE, key);
    }

    /**
     * 检查元素是否可能存在于指定命名空间的布隆过滤器
     * 
     * @param namespace 命名空间
     * @param key 键
     * @return true 如果可能存在，false 如果一定不存在
     */
    public boolean mightContain(String namespace, String key) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Attempted to check null or empty key in bloom filter");
            return false;
        }
        
        BloomFilter<String> bloomFilter = getOrCreateBloomFilter(namespace);
        boolean result = bloomFilter.mightContain(key);
        
        log.debug("Bloom filter check: namespace={}, key={}, result={}", namespace, key, result);
        
        return result;
    }

    /**
     * 获取布隆过滤器的预期误判率
     * 
     * @param namespace 命名空间
     * @return 预期误判率
     */
    public double getExpectedFalsePositiveProbability(String namespace) {
        BloomFilter<String> bloomFilter = bloomFilters.get(namespace);
        if (bloomFilter == null) {
            return DEFAULT_FALSE_POSITIVE_PROBABILITY;
        }
        return bloomFilter.expectedFpp();
    }

    /**
     * 获取默认布隆过滤器的预期误判率
     * 
     * @return 预期误判率
     */
    public double getExpectedFalsePositiveProbability() {
        return getExpectedFalsePositiveProbability(DEFAULT_NAMESPACE);
    }

    /**
     * 清空指定命名空间的布隆过滤器
     * 
     * @param namespace 命名空间
     */
    public void clear(String namespace) {
        BloomFilter<String> bloomFilter = bloomFilters.get(namespace);
        if (bloomFilter != null) {
            // 重新创建布隆过滤器
            createBloomFilter(namespace, DEFAULT_EXPECTED_INSERTIONS, DEFAULT_FALSE_POSITIVE_PROBABILITY);
            log.info("Cleared bloom filter for namespace: {}", namespace);
        }
    }

    /**
     * 清空默认布隆过滤器
     */
    public void clear() {
        clear(DEFAULT_NAMESPACE);
    }

    /**
     * 删除指定命名空间的布隆过滤器
     * 
     * @param namespace 命名空间
     */
    public void remove(String namespace) {
        BloomFilter<String> removed = bloomFilters.remove(namespace);
        if (removed != null) {
            log.info("Removed bloom filter for namespace: {}", namespace);
        }
    }

    /**
     * 获取或创建布隆过滤器
     * 
     * @param namespace 命名空间
     * @return 布隆过滤器
     */
    private BloomFilter<String> getOrCreateBloomFilter(String namespace) {
        return bloomFilters.computeIfAbsent(namespace, ns -> {
            log.info("Creating bloom filter for namespace: {}", ns);
            return BloomFilter.create(
                    Funnels.stringFunnel(StandardCharsets.UTF_8),
                    DEFAULT_EXPECTED_INSERTIONS,
                    DEFAULT_FALSE_POSITIVE_PROBABILITY
            );
        });
    }

    /**
     * 获取所有命名空间
     * 
     * @return 命名空间集合
     */
    public java.util.Set<String> getNamespaces() {
        return bloomFilters.keySet();
    }

    /**
     * 获取布隆过滤器数量
     * 
     * @return 布隆过滤器数量
     */
    public int getBloomFilterCount() {
        return bloomFilters.size();
    }
}
