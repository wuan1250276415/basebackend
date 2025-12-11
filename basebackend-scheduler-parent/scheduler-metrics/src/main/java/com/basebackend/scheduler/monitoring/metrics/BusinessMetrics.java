//package com.basebackend.scheduler.monitoring.metrics;
//
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.DistributionSummary;
//import io.micrometer.core.instrument.Gauge;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.Map;
//
///**
// * 业务指标监控
// *
// * <p>提供全方位的业务层指标监控：
// * <ul>
// *   <li>工作流执行指标（成功率、失败率、吞吐量）</li>
// *   <li>任务处理指标（处理时间、并发数）</li>
// *   <li>业务操作指标（订单、邮件、数据同步等）</li>
// *   <li>系统健康指标（活跃实例、队列长度）</li>
// * </ul>
// *
// * @author BaseBackend Team
// * @version 1.0.0
// * @since 2025-01-01
// */
//@Component
//public class BusinessMetrics {
//
//    private final MeterRegistry meterRegistry;
//
//    // ========== 工作流执行指标 ==========
//    private final Counter workflowStartedCounter;
//    private final Counter workflowCompletedCounter;
//    private final Counter workflowFailedCounter;
//    private final Counter workflowCancelledCounter;
//    private final Timer workflowExecutionTimer;
//    private final DistributionSummary workflowExecutionDuration;
//    private final AtomicLong activeWorkflowInstances = new AtomicLong(0);
//    private final AtomicLong failedWorkflowInstances = new AtomicLong(0);
//
//    // ========== 任务处理指标 ==========
//    private final Counter taskProcessedCounter;
//    private final Counter taskSucceededCounter;
//    private final Counter taskFailedCounter;
//    private final Timer taskProcessingTimer;
//    private final DistributionSummary taskProcessingDuration;
//    private final AtomicLong activeTasks = new AtomicLong(0);
//
//    // ========== 业务操作指标 ==========
//    private final Counter orderApprovalCounter;
//    private final Counter orderRejectedCounter;
//    private final Counter emailSentCounter;
//    private final Counter emailFailedCounter;
//    private final Counter dataSyncCounter;
//    private final Counter dataSyncFailedCounter;
//    private final Counter microserviceCallCounter;
//    private final Counter microserviceCallFailedCounter;
//
//    private final Timer orderProcessingTimer;
//    private final Timer emailDeliveryTimer;
//    private final Timer dataSyncTimer;
//    private final Timer microserviceCallTimer;
//
//    private final DistributionSummary orderAmountDistribution;
//    private final DistributionSummary dataTransferSize;
//
//    private final AtomicLong pendingOrders = new AtomicLong(0);
//    private final AtomicLong inProgressOrders = new AtomicLong(0);
//    private final AtomicLong pendingEmailQueue = new AtomicLong(0);
//    private final AtomicLong pendingSyncTasks = new AtomicLong(0);
//
//    // ========== 工作流类型统计 ==========
//    private final Map<String, AtomicLong> workflowTypeStartedCounts = new ConcurrentHashMap<>();
//    private final Map<String, AtomicLong> workflowTypeCompletedCounts = new ConcurrentHashMap<>();
//    private final Map<String, AtomicLong> workflowTypeFailedCounts = new ConcurrentHashMap<>();
//
//    public BusinessMetrics(MeterRegistry meterRegistry) {
//        this.meterRegistry = meterRegistry;
//
//        // 初始化工作流执行指标
//        this.workflowStartedCounter = Counter.builder("workflow_instance_started_total")
//                .description("Total number of started workflow instances")
//                .register(meterRegistry);
//
//        this.workflowCompletedCounter = Counter.builder("workflow_instance_completed_total")
//                .description("Total number of completed workflow instances")
//                .register(meterRegistry);
//
//        this.workflowFailedCounter = Counter.builder("workflow_instance_failed_total")
//                .description("Total number of failed workflow instances")
//                .register(meterRegistry);
//
//        this.workflowCancelledCounter = Counter.builder("workflow_instance_cancelled_total")
//                .description("Total number of cancelled workflow instances")
//                .register(meterRegistry);
//
//        this.workflowExecutionTimer = Timer.builder("workflow_execution_seconds")
//                .description("Time spent executing workflow instances")
//                .register(meterRegistry);
//
//        this.workflowExecutionDuration = DistributionSummary.builder("workflow_execution_duration_seconds")
//                .description("Duration of workflow executions")
//                .register(meterRegistry);
//
//        // 初始化任务处理指标
//        this.taskProcessedCounter = Counter.builder("workflow_task_processed_total")
//                .description("Total number of processed tasks")
//                .register(meterRegistry);
//
//        this.taskSucceededCounter = Counter.builder("workflow_task_succeeded_total")
//                .description("Total number of succeeded tasks")
//                .register(meterRegistry);
//
//        this.taskFailedCounter = Counter.builder("workflow_task_failed_total")
//                .description("Total number of failed tasks")
//                .register(meterRegistry);
//
//        this.taskProcessingTimer = Timer.builder("workflow_task_processing_seconds")
//                .description("Time spent processing tasks")
//                .register(meterRegistry);
//
//        this.taskProcessingDuration = DistributionSummary.builder("workflow_task_processing_duration_seconds")
//                .description("Duration of task processing")
//                .register(meterRegistry);
//
//        // 初始化业务操作指标
//        this.orderApprovalCounter = Counter.builder("business_order_approval_total")
//                .description("Total number of approved orders")
//                .register(meterRegistry);
//
//        this.orderRejectedCounter = Counter.builder("business_order_rejected_total")
//                .description("Total number of rejected orders")
//                .register(meterRegistry);
//
//        this.emailSentCounter = Counter.builder("business_email_sent_total")
//                .description("Total number of successfully sent emails")
//                .register(meterRegistry);
//
//        this.emailFailedCounter = Counter.builder("business_email_failed_total")
//                .description("Total number of failed email deliveries")
//                .register(meterRegistry);
//
//        this.dataSyncCounter = Counter.builder("business_data_sync_total")
//                .description("Total number of successful data synchronizations")
//                .register(meterRegistry);
//
//        this.dataSyncFailedCounter = Counter.builder("business_data_sync_failed_total")
//                .description("Total number of failed data synchronizations")
//                .register(meterRegistry);
//
//        this.microserviceCallCounter = Counter.builder("business_microservice_call_total")
//                .description("Total number of successful microservice calls")
//                .register(meterRegistry);
//
//        this.microserviceCallFailedCounter = Counter.builder("business_microservice_call_failed_total")
//                .description("Total number of failed microservice calls")
//                .register(meterRegistry);
//
//        this.orderProcessingTimer = Timer.builder("business_order_processing_seconds")
//                .description("Time spent processing orders")
//                .register(meterRegistry);
//
//        this.emailDeliveryTimer = Timer.builder("business_email_delivery_seconds")
//                .description("Time spent delivering emails")
//                .register(meterRegistry);
//
//        this.dataSyncTimer = Timer.builder("business_data_sync_seconds")
//                .description("Time spent synchronizing data")
//                .register(meterRegistry);
//
//        this.microserviceCallTimer = Timer.builder("business_microservice_call_seconds")
//                .description("Time spent in microservice calls")
//                .register(meterRegistry);
//
//        this.orderAmountDistribution = DistributionSummary.builder("business_order_amount")
//                .description("Distribution of order amounts")
//                .register(meterRegistry);
//
//        this.dataTransferSize = DistributionSummary.builder("business_data_transfer_bytes")
//                .description("Size of data transferred during sync operations")
//                .register(meterRegistry);
//
//        // 注册Gauge指标
//        Gauge.builder("workflow_active_instances", activeWorkflowInstances, AtomicLong::doubleValue)
//                .description("Current number of active workflow instances")
//                .register(meterRegistry);
//
//        Gauge.builder("workflow_failed_instances", failedWorkflowInstances, AtomicLong::doubleValue)
//                .description("Current number of failed workflow instances")
//                .register(meterRegistry);
//
//        Gauge.builder("workflow_active_tasks", activeTasks, AtomicLong::doubleValue)
//                .description("Current number of active tasks")
//                .register(meterRegistry);
//
//        Gauge.builder("business_pending_orders", pendingOrders, AtomicLong::doubleValue)
//                .description("Current number of pending orders")
//                .register(meterRegistry);
//
//        Gauge.builder("business_in_progress_orders", inProgressOrders, AtomicLong::doubleValue)
//                .description("Current number of orders in progress")
//                .register(meterRegistry);
//
//        Gauge.builder("business_pending_email_queue", pendingEmailQueue, AtomicLong::doubleValue)
//                .description("Current size of pending email queue")
//                .register(meterRegistry);
//
//        Gauge.builder("business_pending_sync_tasks", pendingSyncTasks, AtomicLong::doubleValue)
//                .description("Current number of pending sync tasks")
//                .register(meterRegistry);
//    }
//
//    // ========== 工作流指标方法 ==========
//
//    /**
//     * 记录工作流实例启动
//     */
//    public void recordWorkflowStarted(String workflowType) {
//        workflowStartedCounter.increment();
//        activeWorkflowInstances.incrementAndGet();
//
//        // 按类型统计
//        workflowTypeStartedCounts.computeIfAbsent(workflowType, k -> new AtomicLong(0)).incrementAndGet();
//    }
//
//    /**
//     * 记录工作流实例完成
//     */
//    public void recordWorkflowCompleted(String workflowType, long durationMillis) {
//        workflowCompletedCounter.increment();
//        activeWorkflowInstances.decrementAndGet();
//        workflowExecutionDuration.record(durationMillis / 1000.0);
//
//        // 按类型统计
//        workflowTypeCompletedCounts.computeIfAbsent(workflowType, k -> new AtomicLong(0)).incrementAndGet();
//    }
//
//    /**
//     * 记录工作流实例失败
//     */
//    public void recordWorkflowFailed(String workflowType) {
//        workflowFailedCounter.increment();
//        activeWorkflowInstances.decrementAndGet();
//        failedWorkflowInstances.incrementAndGet();
//
//        // 按类型统计
//        workflowTypeFailedCounts.computeIfAbsent(workflowType, k -> new AtomicLong(0)).incrementAndGet();
//    }
//
//    /**
//     * 记录工作流实例取消
//     */
//    public void recordWorkflowCancelled(String workflowType) {
//        workflowCancelledCounter.increment();
//        activeWorkflowInstances.decrementAndGet();
//    }
//
//    /**
//     * 记录工作流执行时间
//     */
//    public Timer.Sample startWorkflowExecutionTimer() {
//        return Timer.start();
//    }
//
//    public void stopWorkflowExecutionTimer(Timer.Sample sample) {
//        sample.stop(workflowExecutionTimer);
//    }
//
//    // ========== 任务指标方法 ==========
//
//    /**
//     * 记录任务处理
//     */
//    public void recordTaskProcessed() {
//        taskProcessedCounter.increment();
//        activeTasks.incrementAndGet();
//    }
//
//    /**
//     * 记录任务成功
//     */
//    public void recordTaskSucceeded(long durationMillis) {
//        taskSucceededCounter.increment();
//        activeTasks.decrementAndGet();
//        taskProcessingDuration.record(durationMillis / 1000.0);
//    }
//
//    /**
//     * 记录任务失败
//     */
//    public void recordTaskFailed(long durationMillis) {
//        taskFailedCounter.increment();
//        activeTasks.decrementAndGet();
//        taskProcessingDuration.record(durationMillis / 1000.0);
//    }
//
//    /**
//     * 记录任务处理时间
//     */
//    public Timer.Sample startTaskProcessingTimer() {
//        return Timer.start();
//    }
//
//    public void stopTaskProcessingTimer(Timer.Sample sample) {
//        sample.stop(taskProcessingTimer);
//    }
//
//    // ========== 业务操作方法 ==========
//
//    public void recordOrderApproved(double amount) {
//        orderApprovalCounter.increment();
//        pendingOrders.decrementAndGet();
//        orderAmountDistribution.record(amount);
//    }
//
//    public void recordOrderRejected() {
//        orderRejectedCounter.increment();
//        pendingOrders.decrementAndGet();
//    }
//
//    public void recordOrderSubmitted() {
//        pendingOrders.incrementAndGet();
//        inProgressOrders.incrementAndGet();
//    }
//
//    public void recordOrderCompleted() {
//        inProgressOrders.decrementAndGet();
//    }
//
//    public void recordEmailSent() {
//        emailSentCounter.increment();
//        pendingEmailQueue.decrementAndGet();
//    }
//
//    public void recordEmailFailed() {
//        emailFailedCounter.increment();
//        pendingEmailQueue.decrementAndGet();
//    }
//
//    public void recordEmailQueued() {
//        pendingEmailQueue.incrementAndGet();
//    }
//
//    public void recordDataSyncSuccess(long sizeBytes) {
//        dataSyncCounter.increment();
//        pendingSyncTasks.decrementAndGet();
//        dataTransferSize.record(sizeBytes);
//    }
//
//    public void recordDataSyncFailed() {
//        dataSyncFailedCounter.increment();
//        pendingSyncTasks.decrementAndGet();
//    }
//
//    public void recordDataSyncStarted() {
//        pendingSyncTasks.incrementAndGet();
//    }
//
//    public void recordMicroserviceCallSuccess() {
//        microserviceCallCounter.increment();
//    }
//
//    public void recordMicroserviceCallFailed() {
//        microserviceCallFailedCounter.increment();
//    }
//
//    public Timer.Sample startOrderProcessingTimer() {
//        return Timer.start();
//    }
//
//    public void stopOrderProcessingTimer(Timer.Sample sample) {
//        sample.stop(orderProcessingTimer);
//    }
//
//    public Timer.Sample startEmailDeliveryTimer() {
//        return Timer.start();
//    }
//
//    public void stopEmailDeliveryTimer(Timer.Sample sample) {
//        sample.stop(emailDeliveryTimer);
//    }
//
//    public Timer.Sample startDataSyncTimer() {
//        return Timer.start();
//    }
//
//    public void stopDataSyncTimer(Timer.Sample sample) {
//        sample.stop(dataSyncTimer);
//    }
//
//    public Timer.Sample startMicroserviceCallTimer() {
//        return Timer.start();
//    }
//
//    public void stopMicroserviceCallTimer(Timer.Sample sample) {
//        sample.stop(microserviceCallTimer);
//    }
//
//    // ========== 统计查询方法 ==========
//
//    /**
//     * 获取工作流成功率
//     */
//    public double getWorkflowSuccessRate() {
//        double completed = workflowCompletedCounter.count();
//        double failed = workflowFailedCounter.count();
//        double total = completed + failed;
//        return total > 0 ? completed / total * 100 : 0;
//    }
//
//    /**
//     * 获取工作流失败率
//     */
//    public double getWorkflowFailureRate() {
//        double completed = workflowCompletedCounter.count();
//        double failed = workflowFailedCounter.count();
//        double total = completed + failed;
//        return total > 0 ? failed / total * 100 : 0;
//    }
//
//    /**
//     * 获取任务成功率
//     */
//    public double getTaskSuccessRate() {
//        double succeeded = taskSucceededCounter.count();
//        double failed = taskFailedCounter.count();
//        double total = succeeded + failed;
//        return total > 0 ? succeeded / total * 100 : 0;
//    }
//
//    /**
//     * 获取指定类型工作流的统计数据
//     */
//    public Map<String, Object> getWorkflowTypeStats(String workflowType) {
//        Map<String, Object> stats = new ConcurrentHashMap<>();
//        AtomicLong started = workflowTypeStartedCounts.get(workflowType);
//        AtomicLong completed = workflowTypeCompletedCounts.get(workflowType);
//        AtomicLong failed = workflowTypeFailedCounts.get(workflowType);
//
//        long startedCount = started != null ? started.get() : 0L;
//        long completedCount = completed != null ? completed.get() : 0L;
//        long failedCount = failed != null ? failed.get() : 0L;
//
//        stats.put("started", startedCount);
//        stats.put("completed", completedCount);
//        stats.put("failed", failedCount);
//
//        long total = completedCount + failedCount;
//        double successRate = total > 0 ? (double) completedCount / total * 100 : 0.0;
//        stats.put("successRate", successRate);
//
//        return stats;
//    }
//}
