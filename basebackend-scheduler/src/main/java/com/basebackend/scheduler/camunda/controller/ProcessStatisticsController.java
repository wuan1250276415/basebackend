package com.basebackend.scheduler.camunda.controller;

import com.basebackend.scheduler.camunda.dto.ProcessStatisticsDTO;
import com.basebackend.scheduler.camunda.service.ProcessStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程统计分析接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow/statistics")
@RequiredArgsConstructor
public class ProcessStatisticsController {

    private final ProcessStatisticsService statisticsService;

    /**
     * 获取流程总体统计信息
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getProcessStatistics() {
        ProcessStatisticsDTO statistics = statisticsService.getProcessStatistics();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", statistics);

        return ResponseEntity.ok(response);
    }

    /**
     * 按流程定义统计
     */
    @GetMapping("/by-definition")
    public ResponseEntity<Map<String, Object>> getStatisticsByDefinition() {
        List<ProcessStatisticsDTO.DefinitionStatistics> statistics =
                statisticsService.getStatisticsByDefinition();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", statistics);
        response.put("total", statistics.size());

        return ResponseEntity.ok(response);
    }
}
