package com.basebackend.scheduler.registry;

import java.util.Set;

/**
 * 任务注册表接口
 * <p>
 * 定义了PowerJob任务处理器的注册、查询、管理等操作的接口规范。
 * 提供统一的任务管理入口，支持任务的动态注册、取消注册、查询等操作。
 * </p>
 *
 * <h3>主要功能:</h3>
 * <ul>
 *   <li><b>任务注册</b>: registerProcessor() - 注册任务处理器</li>
 *   <li><b>任务查询</b>: getProcessor() - 根据任务名称获取处理器</li>
 *   <li><b>任务列表</b>: getRegisteredJobs() - 获取所有已注册的任务</li>
 *   <li><b>任务移除</b>: unregisterProcessor() - 取消注册任务处理器</li>
 *   <li><b>批量操作</b>: registerAll() - 批量注册任务处理器</li>
 *   <li><b>状态检查</b>: isRegistered() - 检查任务是否已注册</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-11-25
 */
public interface JobRegistry {

    /**
     * 注册任务处理器
     */
    void registerProcessor(String jobName, Class<?> processorClass);

    /**
     * 注册任务处理器实例
     */
    void registerProcessorInstance(String jobName, Object processor);

    /**
     * 根据任务名称获取处理器类
     */
    Class<?> getProcessorClass(String jobName);

    /**
     * 根据任务名称获取处理器实例
     */
    Object getProcessorInstance(String jobName);

    /**
     * 获取所有已注册的任务名称列表
     */
    Set<String> getRegisteredJobs();

    /**
     * 检查任务是否已注册
     */
    boolean isRegistered(String jobName);

    /**
     * 取消注册任务处理器
     */
    boolean unregisterProcessor(String jobName);

    /**
     * 批量注册任务处理器
     */
    BatchRegisterResult registerAll(java.util.Map<String, Class<?>> processors);

    /**
     * 清空所有注册的任务
     */
    int clearAll();

    /**
     * 获取注册任务的数量
     */
    int getRegisteredJobCount();

    /**
     * 获取任务注册表的统计信息
     */
    RegistryStatistics getStatistics();

    /**
     * 批量注册结果
     */
    class BatchRegisterResult {
        private final int total;
        private final int successCount;
        private final int failureCount;
        private final java.util.List<String> failureReasons;

        public BatchRegisterResult(int total, int successCount, int failureCount,
                                   java.util.List<String> failureReasons) {
            this.total = total;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.failureReasons = java.util.Collections.unmodifiableList(failureReasons);
        }

        public int getTotal() {
            return total;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public java.util.List<String> getFailureReasons() {
            return failureReasons;
        }

        public boolean isAllSuccess() {
            return failureCount == 0;
        }

        @Override
        public String toString() {
            return String.format("BatchRegisterResult{total=%d, success=%d, failure=%d}",
                    total, successCount, failureCount);
        }
    }

    /**
     * 注册表统计信息
     */
    class RegistryStatistics {
        private final int registeredJobCount;
        private final long memoryUsageBytes;
        private final long lastRegistrationTime;
        private final int totalRegistrations;
        private final int totalUnregistrations;

        public RegistryStatistics(int registeredJobCount, long memoryUsageBytes,
                                  long lastRegistrationTime, int totalRegistrations,
                                  int totalUnregistrations) {
            this.registeredJobCount = registeredJobCount;
            this.memoryUsageBytes = memoryUsageBytes;
            this.lastRegistrationTime = lastRegistrationTime;
            this.totalRegistrations = totalRegistrations;
            this.totalUnregistrations = totalUnregistrations;
        }

        public int getRegisteredJobCount() {
            return registeredJobCount;
        }

        public long getMemoryUsageBytes() {
            return memoryUsageBytes;
        }

        public long getLastRegistrationTime() {
            return lastRegistrationTime;
        }

        public int getTotalRegistrations() {
            return totalRegistrations;
        }

        public int getTotalUnregistrations() {
            return totalUnregistrations;
        }

        public int getActiveJobCount() {
            return totalRegistrations - totalUnregistrations;
        }

        @Override
        public String toString() {
            return String.format(
                    "RegistryStatistics{registered=%d, memory=%d KB, totalReg=%d, totalUnreg=%d}",
                    registeredJobCount,
                    memoryUsageBytes / 1024,
                    totalRegistrations,
                    totalUnregistrations);
        }
    }
}
