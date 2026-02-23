package com.basebackend.scheduler.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 工作流监控指标
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Component
public class WorkflowMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter processStartedCounter;
    private final Counter processCompletedCounter;
    private final Counter processFailedCounter;
    private final Counter processTerminatedCounter;
    
    private final Counter taskCreatedCounter;
    private final Counter taskCompletedCounter;
    private final Counter taskCancelledCounter;
    private final Timer processExecutionTimer;
    private final Timer taskExecutionTimer;
    
    private final DistributionSummary processInstanceDuration;
    private final DistributionSummary taskProcessingDuration;
    
    private final AtomicLong activeProcessInstances = new AtomicLong(0);
    private final AtomicLong activeTasks = new AtomicLong(0);
    private final AtomicLong failedProcessInstances = new AtomicLong(0);

    public WorkflowMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.processStartedCounter = Counter.builder("workflow_process_started_total")
                .description("Total number of started process instances")
                .register(meterRegistry);
        
        this.processCompletedCounter = Counter.builder("workflow_process_completed_total")
                .description("Total number of completed process instances")
                .register(meterRegistry);
        
        this.processFailedCounter = Counter.builder("workflow_process_failed_total")
                .description("Total number of failed process instances")
                .register(meterRegistry);
        
        this.processTerminatedCounter = Counter.builder("workflow_process_terminated_total")
                .description("Total number of terminated process instances")
                .register(meterRegistry);
        
        this.taskCreatedCounter = Counter.builder("workflow_task_created_total")
                .description("Total number of created tasks")
                .register(meterRegistry);
        
        this.taskCompletedCounter = Counter.builder("workflow_task_completed_total")
                .description("Total number of completed tasks")
                .register(meterRegistry);
        
        this.taskCancelledCounter = Counter.builder("workflow_task_cancelled_total")
                .description("Total number of cancelled tasks")
                .register(meterRegistry);
        
        this.processExecutionTimer = Timer.builder("workflow_process_execution_seconds")
                .description("Time spent executing process instances")
                .register(meterRegistry);
        
        this.taskExecutionTimer = Timer.builder("workflow_task_execution_seconds")
                .description("Time spent executing tasks")
                .register(meterRegistry);
        
        this.processInstanceDuration = DistributionSummary.builder("workflow_process_instance_duration_seconds")
                .description("Duration of process instances")
                .register(meterRegistry);
        
        this.taskProcessingDuration = DistributionSummary.builder("workflow_task_processing_seconds")
                .description("Time taken to process tasks")
                .register(meterRegistry);
        
        Gauge.builder("workflow_active_process_instances", activeProcessInstances, AtomicLong::doubleValue)
                .description("Current number of active process instances")
                .register(meterRegistry);
        
        Gauge.builder("workflow_active_tasks", activeTasks, AtomicLong::doubleValue)
                .description("Current number of active tasks")
                .register(meterRegistry);
        
        Gauge.builder("workflow_failed_process_instances", failedProcessInstances, AtomicLong::doubleValue)
                .description("Current number of failed process instances")
                .register(meterRegistry);
    }

    public void recordProcessStarted(ProcessInstance instance) {
        processStartedCounter.increment();
        activeProcessInstances.incrementAndGet();
    }

    public void recordProcessCompleted(ProcessInstance instance, long durationMillis) {
        processCompletedCounter.increment();
        activeProcessInstances.decrementAndGet();
        processInstanceDuration.record(durationMillis / 1000.0);
    }

    public void recordProcessFailed(ProcessInstance instance) {
        processFailedCounter.increment();
        activeProcessInstances.decrementAndGet();
        failedProcessInstances.incrementAndGet();
    }

    public void recordProcessTerminated(ProcessInstance instance) {
        processTerminatedCounter.increment();
        activeProcessInstances.decrementAndGet();
    }

    public void recordTaskCreated(Task task) {
        taskCreatedCounter.increment();
        activeTasks.incrementAndGet();
    }

    public void recordTaskCompleted(Task task, long durationMillis) {
        taskCompletedCounter.increment();
        activeTasks.decrementAndGet();
        taskProcessingDuration.record(durationMillis / 1000.0);
    }

    public void recordTaskCancelled(Task task) {
        taskCancelledCounter.increment();
        activeTasks.decrementAndGet();
    }

    public Timer.Sample startProcessExecutionTimer() {
        return Timer.start();
    }

    public void stopProcessExecutionTimer(Timer.Sample sample) {
        sample.stop(processExecutionTimer);
    }

    public Timer.Sample startTaskExecutionTimer() {
        return Timer.start();
    }

    public void stopTaskExecutionTimer(Timer.Sample sample) {
        sample.stop(taskExecutionTimer);
    }
    /**
     * 获取 MeterRegistry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}
