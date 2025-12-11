package com.basebackend.file.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储服务注册中心
 *
 * 负责管理所有存储服务的注册和路由
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageServiceRegistry {

    private final Map<StorageService.StorageType, StorageService> storageServiceMap = new ConcurrentHashMap<>();

    /**
     * 注册存储服务
     *
     * @param storageType 存储类型
     * @param storageService 存储服务实例
     */
    public void registerService(StorageService.StorageType storageType, StorageService storageService) {
        log.info("注册存储服务: {}", storageType.getDescription());
        storageServiceMap.put(storageType, storageService);
    }

    /**
     * 获取存储服务
     *
     * @param storageType 存储类型
     * @return 存储服务实例
     * @throws IllegalArgumentException 如果未找到对应的存储服务
     */
    public StorageService getService(StorageService.StorageType storageType) {
        StorageService service = storageServiceMap.get(storageType);
        if (service == null) {
            log.error("未找到存储服务: {}", storageType);
            throw new IllegalArgumentException("未找到存储服务: " + storageType);
        }
        return service;
    }

    /**
     * 检查是否已注册指定存储类型
     *
     * @param storageType 存储类型
     * @return 是否已注册
     */
    public boolean hasService(StorageService.StorageType storageType) {
        return storageServiceMap.containsKey(storageType);
    }

    /**
     * 获取所有已注册的存储类型
     *
     * @return 存储类型列表
     */
    public java.util.List<StorageService.StorageType> getRegisteredTypes() {
        return new java.util.ArrayList<>(storageServiceMap.keySet());
    }

    /**
     * 获取默认存储服务（第一个注册的服务）
     *
     * @return 默认存储服务
     */
    public StorageService getDefaultService() {
        if (storageServiceMap.isEmpty()) {
            log.error("没有已注册的存储服务");
            throw new IllegalStateException("没有已注册的存储服务");
        }
        StorageService service = storageServiceMap.values().iterator().next();
        log.info("获取默认存储服务: {}", service.getStorageType().getDescription());
        return service;
    }

    /**
     * 初始化后处理
     * 打印注册的服务列表
     */
    @PostConstruct
    public void init() {
        log.info("存储服务注册中心初始化完成");
        log.info("已注册的存储服务: {}", getRegisteredTypes().size());
        for (StorageService.StorageType type : getRegisteredTypes()) {
            log.info("- {}", type.getDescription());
        }
    }
}
