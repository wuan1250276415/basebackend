package com.basebackend.scheduler.camunda.service;

import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricProcessInstanceQuery;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 历史流程实例查询服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricProcessInstanceService {

    private final HistoryService historyService;

    /**
     * 查询历史流程实例列表（支持分页和筛选）
     */
    public Map<String, Object> listHistoricProcessInstances(
            String processDefinitionKey,
            String businessKey,
            Date startedAfter,
            Date startedBefore,
            Date finishedAfter,
            Date finishedBefore,
            Boolean finished,
            Integer page,
            Integer size) {

        // 构建查询
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

        // 应用筛选条件
        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }
        if (businessKey != null && !businessKey.isEmpty()) {
            query.processInstanceBusinessKey(businessKey);
        }
        if (startedAfter != null) {
            query.startedAfter(startedAfter);
        }
        if (startedBefore != null) {
            query.startedBefore(startedBefore);
        }
        if (finishedAfter != null) {
            query.finishedAfter(finishedAfter);
        }
        if (finishedBefore != null) {
            query.finishedBefore(finishedBefore);
        }
        if (finished != null) {
            if (finished) {
                query.finished();
            } else {
                query.unfinished();
            }
        }

        // 获取总数
        long total = query.count();

        // 分页查询
        int pageNum = (page != null && page > 0) ? page : 1;
        int pageSize = (size != null && size > 0) ? size : 10;
        int firstResult = (pageNum - 1) * pageSize;

        List<HistoricProcessInstance> historicInstances = query
                .orderByProcessInstanceStartTime()
                .desc()
                .listPage(firstResult, pageSize);

        // 转换为DTO
        List<HistoricProcessInstanceDTO> dtoList = historicInstances.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("list", dtoList);
        result.put("total", total);
        result.put("page", pageNum);
        result.put("size", pageSize);
        result.put("pages", (int) Math.ceil((double) total / pageSize));

        return result;
    }

    /**
     * 根据ID查询历史流程实例详情
     */
    public HistoricProcessInstanceDTO getHistoricProcessInstanceById(String processInstanceId) {
        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance == null) {
            throw new RuntimeException("历史流程实例不存在: id=" + processInstanceId);
        }

        return convertToDTO(historicInstance, true);
    }

    /**
     * 查询历史流程实例的活动历史
     */
    public List<HistoricActivityInstanceDTO> getHistoricActivities(String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        return activities.stream()
                .map(this::convertToActivityDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为DTO（不包含变量）
     */
    private HistoricProcessInstanceDTO convertToDTO(HistoricProcessInstance hpi) {
        return convertToDTO(hpi, false);
    }

    /**
     * 转换为DTO
     * @param hpi 历史流程实例
     * @param includeVariables 是否包含流程变量
     */
    private HistoricProcessInstanceDTO convertToDTO(HistoricProcessInstance hpi, boolean includeVariables) {
        Long duration = null;
        if (hpi.getStartTime() != null && hpi.getEndTime() != null) {
            duration = hpi.getEndTime().getTime() - hpi.getStartTime().getTime();
        }

        HistoricProcessInstanceDTO.HistoricProcessInstanceDTOBuilder builder = HistoricProcessInstanceDTO.builder()
                .id(hpi.getId())
                .businessKey(hpi.getBusinessKey())
                .processDefinitionId(hpi.getProcessDefinitionId())
                .processDefinitionKey(hpi.getProcessDefinitionKey())
                .processDefinitionName(hpi.getProcessDefinitionName())
                .processDefinitionVersion(hpi.getProcessDefinitionVersion())
                .startTime(hpi.getStartTime())
                .endTime(hpi.getEndTime())
                .durationInMillis(duration)
                .startActivityId(hpi.getStartActivityId())
                .endActivityId(hpi.getEndActivityId())
                .startUserId(hpi.getStartUserId())
                .deleteReason(hpi.getDeleteReason())
                .state(hpi.getState())
                .tenantId(hpi.getTenantId());

        // 如果需要包含变量，查询并添加
        if (includeVariables) {
            Map<String, Object> variables = getHistoricVariables(hpi.getId());
            builder.variables(variables);
        }

        return builder.build();
    }

    /**
     * 获取历史流程变量
     */
    private Map<String, Object> getHistoricVariables(String processInstanceId) {
        List<HistoricVariableInstance> variableInstances = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        Map<String, Object> variables = new HashMap<>();
        for (HistoricVariableInstance variable : variableInstances) {
            variables.put(variable.getName(), variable.getValue());
        }

        return variables;
    }

    /**
     * 转换为活动实例DTO
     */
    private HistoricActivityInstanceDTO convertToActivityDTO(HistoricActivityInstance hai) {
        Long duration = null;
        if (hai.getStartTime() != null && hai.getEndTime() != null) {
            duration = hai.getEndTime().getTime() - hai.getStartTime().getTime();
        }

        return HistoricActivityInstanceDTO.builder()
                .id(hai.getId())
                .activityId(hai.getActivityId())
                .activityName(hai.getActivityName())
                .activityType(hai.getActivityType())
                .processDefinitionId(hai.getProcessDefinitionId())
                .processDefinitionKey(hai.getProcessDefinitionKey())
                .processInstanceId(hai.getProcessInstanceId())
                .executionId(hai.getExecutionId())
                .taskId(hai.getTaskId())
                .assignee(hai.getAssignee())
                .startTime(hai.getStartTime())
                .endTime(hai.getEndTime())
                .durationInMillis(duration)
                .deleteReason(null) // HistoricActivityInstance没有deleteReason字段
                .tenantId(hai.getTenantId())
                .build();
    }
}
