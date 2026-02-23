package com.basebackend.scheduler.monitoring.health;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Camunda引擎健康检查
 * 
 * @author BaseBackend Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Component
public class CamundaEngineHealthIndicator implements HealthIndicator {

    private final ProcessEngine processEngine;

    public CamundaEngineHealthIndicator(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @Override
    public Health health() {
        try {
            // 检查数据库连接
            String engineName = processEngine.getName();
            
            // 检查RepositoryService
            long deployedProcessCount = processEngine.getRepositoryService()
                    .createProcessDefinitionQuery()
                    .count();
            
            // 检查RuntimeService
            long activeInstanceCount = processEngine.getRuntimeService()
                    .createProcessInstanceQuery()
                    .active()
                    .count();
            
            // 检查TaskService
            long taskCount = processEngine.getTaskService()
                    .createTaskQuery()
                    .count();
            
            return Health.up()
                    .withDetail("engineName", engineName)
                    .withDetail("deployedProcessCount", deployedProcessCount)
                    .withDetail("activeInstanceCount", activeInstanceCount)
                    .withDetail("taskCount", taskCount)
                    .withDetail("status", "UP")
                    .build();
                    
        } catch (ProcessEngineException e) {
            return Health.down()
                    .withDetail("status", "DOWN")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
