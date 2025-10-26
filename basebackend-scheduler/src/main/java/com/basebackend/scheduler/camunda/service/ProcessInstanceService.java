package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流程实例管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessInstanceService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    /**
     * 启动流程实例
     */
    public ProcessInstanceDTO startProcessInstance(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    processDefinitionKey,
                    businessKey,
                    variables
            );

            log.info("流程实例启动成功: processInstanceId={}, businessKey={}",
                    processInstance.getId(), businessKey);

            return convertToDTO(processInstance, variables);
        } catch (Exception e) {
            log.error("流程实例启动失败: key={}, businessKey={}", processDefinitionKey, businessKey, e);
            throw new RuntimeException("流程实例启动失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询运行中的流程实例
     */
    public List<ProcessInstanceDTO> listRunningProcessInstances() {
        return runtimeService.createProcessInstanceQuery()
                .active()
                .orderByProcessInstanceId()
                .desc()
                .list()
                .stream()
                .map(pi -> convertToDTO(pi, null))
                .collect(Collectors.toList());
    }

    /**
     * 根据流程定义Key查询运行中的流程实例
     */
    public List<ProcessInstanceDTO> listRunningProcessInstancesByKey(String processDefinitionKey) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .orderByProcessInstanceId()
                .desc()
                .list()
                .stream()
                .map(pi -> convertToDTO(pi, null))
                .collect(Collectors.toList());
    }

    /**
     * 根据业务键查询流程实例
     */
    public ProcessInstanceDTO getProcessInstanceByBusinessKey(String businessKey) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (processInstance == null) {
            throw new RuntimeException("流程实例不存在: businessKey=" + businessKey);
        }

        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        return convertToDTO(processInstance, variables);
    }

    /**
     * 根据ID查询流程实例
     */
    public ProcessInstanceDTO getProcessInstanceById(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            // 尝试查询历史实例
            return getHistoricProcessInstanceById(processInstanceId);
        }

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        return convertToDTO(processInstance, variables);
    }

    /**
     * 查询历史流程实例
     */
    public ProcessInstanceDTO getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicProcessInstance == null) {
            throw new RuntimeException("流程实例不存在: id=" + processInstanceId);
        }

        return convertToHistoricDTO(historicProcessInstance);
    }

    /**
     * 挂起流程实例
     */
    public void suspendProcessInstance(String processInstanceId) {
        runtimeService.suspendProcessInstanceById(processInstanceId);
        log.info("流程实例已挂起: id={}", processInstanceId);
    }

    /**
     * 激活流程实例
     */
    public void activateProcessInstance(String processInstanceId) {
        runtimeService.activateProcessInstanceById(processInstanceId);
        log.info("流程实例已激活: id={}", processInstanceId);
    }

    /**
     * 删除流程实例
     */
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
        log.info("流程实例已删除: id={}, reason={}", processInstanceId, deleteReason);
    }

    /**
     * 设置流程变量
     */
    public void setVariables(String processInstanceId, Map<String, Object> variables) {
        runtimeService.setVariables(processInstanceId, variables);
        log.info("流程变量已设置: processInstanceId={}, variables={}", processInstanceId, variables.keySet());
    }

    /**
     * 获取流程变量
     */
    public Map<String, Object> getVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * 转换为DTO
     */
    private ProcessInstanceDTO convertToDTO(ProcessInstance pi, Map<String, Object> variables) {
        return ProcessInstanceDTO.builder()
                .id(pi.getId())
                .businessKey(pi.getBusinessKey())
                .processDefinitionId(pi.getProcessDefinitionId())
                .suspended(pi.isSuspended())
                .ended(pi.isEnded())
                .tenantId(pi.getTenantId())
                .variables(variables != null ? variables : new HashMap<>())
                .build();
    }

    /**
     * 转换历史实例为DTO
     */
    private ProcessInstanceDTO convertToHistoricDTO(HistoricProcessInstance hpi) {
        Long duration = null;
        if (hpi.getStartTime() != null && hpi.getEndTime() != null) {
            duration = hpi.getEndTime().getTime() - hpi.getStartTime().getTime();
        }

        return ProcessInstanceDTO.builder()
                .id(hpi.getId())
                .businessKey(hpi.getBusinessKey())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(hpi.getProcessDefinitionName())
                .ended(hpi.getEndTime() != null)
                .startTime(hpi.getStartTime())
                .endTime(hpi.getEndTime())
                .durationInMillis(duration)
                .tenantId(hpi.getTenantId())
                .build();
    }
}
