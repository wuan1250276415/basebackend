package com.basebackend.scheduler.enums;

/**
 * 执行类型
 */
public enum ExecuteType {
    /**
     * 单机执行 - 随机选择一台机器执行
     */
    STANDALONE("单机执行"),

    /**
     * 广播执行 - 所有Worker都执行
     */
    BROADCAST("广播执行"),

    /**
     * MapReduce执行 - 分布式计算
     */
    MAP_REDUCE("MapReduce执行"),

    /**
     * 分片执行 - 静态分片并行执行
     */
    SHARDING("分片执行");

    private final String description;

    ExecuteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
