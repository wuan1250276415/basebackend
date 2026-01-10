package com.basebackend.scheduler.camunda.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.TaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Camunda 指标配置
 *
 * <p>
 * 负责收集和暴露 Camunda 引擎的关键指标，包括：
 * <ul>
 * <li>活跃流程实例数量</li>
 * <li>运行中的作业数量</li>
 * <li>失败的作业数量</li>
 * <li>任务待办数量</li>
 * </ul>
 *
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CamundaMetricsConfiguration {

    /**
     * 绑定 Camunda 指标到 Micrometer 注册表
     *
     * @param managementService Camunda 管理服务
     * @param taskService       Camunda 任务服务
     * @return MeterBinder 实例
     */
    /**
     * 临时禁用 MeterBinder Bean
     *
     * 注意：此 Bean 由于复杂的 Gauge.builder() 和 Lambda 表达式在运行时会导致
     * Bean 创建失败。为确保应用正常启动，此处暂时禁用。
     *
     * TODO: 后续需要重写此实现，使用更简单的方式创建 Camunda 指标
     */
//    @Bean
    public MeterBinder camundaMeterBinder(ManagementService managementService, TaskService taskService) {
        log.warn("CamundaMeterBinder Bean 已临时禁用，请稍后重写实现");
        return registry -> {
            // 活跃流程实例数量（临时注释）
            // Gauge.builder("camunda.process.instances.active", () -> 0.0)
            //         .description("Active process instances")
            //         .register(registry);

            // 运行中的作业数量（临时注释）
            // Gauge.builder("camunda.jobs.running", managementService, service -> {
            //             try {
            //                 return (double) service.createJobQuery().active().count();
            //             } catch (Exception e) {
            //                 log.debug("Failed to get running job count", e);
            //                 return 0.0;
            //             }
            //         })
            //         .description("Running jobs")
            //         .register(registry);

            // 失败的作业数量（临时注释）
            // Gauge.builder("camunda.jobs.failed", managementService, service -> {
            //             try {
            //                 return (double) service.createJobQuery().withException().count();
            //             } catch (Exception e) {
            //                 log.debug("Failed to get failed job count", e);
            //                 return 0.0;
            //             }
            //         })
            //         .description("Failed jobs")
            //         .register(registry);

            // 任务待办数量（临时注释）
            // Gauge.builder("camunda.tasks.pending", taskService, service -> {
            //             try {
            //                 return (double) service.createTaskQuery().taskUnassigned().count();
            //             } catch (Exception e) {
            //                 log.debug("Failed to get pending task count", e);
            //                 return 0.0;
            //             }
            //         })
            //         .description("Pending tasks")
            //         .register(registry);

            log.info("Camunda metrics configuration 已临时禁用");
        };
    }

    /**
     * 获取活跃流程实例数量
     * 注意：这是一个简化实现，实际应该通过 ManagementService 获取更准确的指标
     */
    private Double getActiveProcessInstanceCount() {
        try {
            // 简化的实现，实际生产环境应该使用更精确的查询
            return 0.0;
        } catch (Exception e) {
            log.debug("Failed to get active process instance count", e);
            return 0.0;
        }
    }

    /**
     * 获取运行中的作业数量
     */
    private Double getRunningJobCount(ManagementService managementService) {
        try {
            return (double) managementService.createJobQuery().active().count();
        } catch (Exception e) {
            log.debug("Failed to get running job count", e);
            return 0.0;
        }
    }

    /**
     * 获取失败的作业数量
     */
    private Double getFailedJobCount(ManagementService managementService) {
        try {
            return (double) managementService.createJobQuery().withException().count();
        } catch (Exception e) {
            log.debug("Failed to get failed job count", e);
            return 0.0;
        }
    }

    /**
     * 获取待办任务数量
     */
    private Double getPendingTaskCount(TaskService taskService) {
        try {
            return (double) taskService.createTaskQuery().taskUnassigned().count();
        } catch (Exception e) {
            log.debug("Failed to get pending task count", e);
            return 0.0;
        }
    }
}
