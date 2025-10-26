package com.basebackend.observability.controller;

import com.basebackend.common.model.Result;
import com.basebackend.observability.trace.model.ServiceTopology;
import com.basebackend.observability.trace.service.ServiceTopologyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 服务拓扑控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/topology")
@RequiredArgsConstructor
@Tag(name = "服务拓扑", description = "服务依赖关系和调用拓扑")
public class TopologyController {

    private final ServiceTopologyService topologyService;

    @GetMapping
    @Operation(summary = "获取服务拓扑图")
    public Result<ServiceTopology> getTopology(
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime startTime,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) 
            LocalDateTime endTime) {
        try {
            // 默认最近1小时
            if (startTime == null) {
                startTime = LocalDateTime.now().minusHours(1);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            ServiceTopology topology = topologyService.generateTopology(startTime, endTime);
            return Result.success(topology);
        } catch (Exception e) {
            log.error("Failed to get topology", e);
            return Result.error(e.getMessage());
        }
    }
}
