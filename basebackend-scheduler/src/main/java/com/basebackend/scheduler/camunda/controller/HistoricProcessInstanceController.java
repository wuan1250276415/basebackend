package com.basebackend.scheduler.camunda.controller;

import com.basebackend.scheduler.camunda.dto.HistoricActivityInstanceDTO;
import com.basebackend.scheduler.camunda.dto.HistoricProcessInstanceDTO;
import com.basebackend.scheduler.camunda.service.HistoricProcessInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史流程实例查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/instances/historic")
@RequiredArgsConstructor
public class HistoricProcessInstanceController {

    private final HistoricProcessInstanceService historicProcessInstanceService;

    /**
     * 查询历史流程实例列表（分页）
     *
     * @param processDefinitionKey 流程定义Key（可选）
     * @param businessKey 业务键（可选）
     * @param startedAfter 开始时间之后（可选）
     * @param startedBefore 开始时间之前（可选）
     * @param finishedAfter 结束时间之后（可选）
     * @param finishedBefore 结束时间之前（可选）
     * @param finished 是否已完成（可选）
     * @param page 页码（默认1）
     * @param size 每页大小（默认10）
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listHistoricProcessInstances(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startedAfter,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startedBefore,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date finishedAfter,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date finishedBefore,
            @RequestParam(required = false) Boolean finished,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {

        Map<String, Object> result = historicProcessInstanceService.listHistoricProcessInstances(
                processDefinitionKey,
                businessKey,
                startedAfter,
                startedBefore,
                finishedAfter,
                finishedBefore,
                finished,
                page,
                size
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result);

        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID查询历史流程实例详情
     *
     * @param id 流程实例ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getHistoricProcessInstanceById(@PathVariable String id) {
        HistoricProcessInstanceDTO instance = historicProcessInstanceService.getHistoricProcessInstanceById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", instance);

        return ResponseEntity.ok(response);
    }

    /**
     * 查询历史流程实例的活动历史
     *
     * @param id 流程实例ID
     */
    @GetMapping("/{id}/activities")
    public ResponseEntity<Map<String, Object>> getHistoricActivities(@PathVariable String id) {
        List<HistoricActivityInstanceDTO> activities = historicProcessInstanceService.getHistoricActivities(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", activities);
        response.put("total", activities.size());

        return ResponseEntity.ok(response);
    }
}
