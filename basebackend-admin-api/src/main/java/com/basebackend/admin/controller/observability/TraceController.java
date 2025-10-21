package com.basebackend.admin.controller.observability;

import com.basebackend.admin.dto.observability.TraceQueryRequest;
import com.basebackend.admin.service.observability.TraceQueryService;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 追踪查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/observability/traces")
@RequiredArgsConstructor
@Validated
@Tag(name = "可观测性-追踪", description = "分布式追踪相关接口")
public class TraceController {

    private final TraceQueryService traceQueryService;

    @GetMapping("/{traceId}")
    @Operation(summary = "根据TraceId查询追踪详情")
    public Result<Map<String, Object>> getTraceById(@PathVariable String traceId) {
        log.info("Querying trace by id: {}", traceId);
        Map<String, Object> trace = traceQueryService.getTraceById(traceId);
        return Result.success(trace);
    }

    @PostMapping("/search")
    @Operation(summary = "搜索追踪")
    public Result<Map<String, Object>> searchTraces(@RequestBody TraceQueryRequest request) {
        log.info("Searching traces for service: {}", request.getServiceName());
        Map<String, Object> result = traceQueryService.searchTraces(request);
        return Result.success(result);
    }

    @GetMapping("/services")
    @Operation(summary = "获取服务列表")
    public Result<List<String>> getServices() {
        List<String> services = traceQueryService.getServices();
        return Result.success(services);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取追踪统计")
    public Result<Map<String, Object>> getTraceStats(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "1") int hours) {
        Map<String, Object> stats = traceQueryService.getTraceStats(serviceName, hours);
        return Result.success(stats);
    }
}
