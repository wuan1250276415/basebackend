package com.basebackend.observability.service.impl;

import com.basebackend.observability.dto.TraceQueryRequest;
import com.basebackend.observability.service.TraceQueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 追踪查询服务实现 - 集成Zipkin/Tempo API
 *
 * @author BaseBackend Team
 * @since 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceQueryServiceImpl implements TraceQueryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${observability.trace.endpoint:http://192.168.66.126:9411}")
    private String traceEndpoint;
    
    @Value("${observability.trace.format:zipkin}")
    private String traceFormat; // zipkin or tempo

    @Override
    public Map<String, Object> getTraceById(String traceId) {
        log.info("Getting trace by id: {} from endpoint: {}, format: {}", traceId, traceEndpoint, traceFormat);

        try {
            // 验证traceId格式
            if (traceId == null || traceId.trim().isEmpty()) {
                log.warn("Invalid traceId: empty or null");
                return createEmptyTrace(traceId);
            }

            String url;
            if ("tempo".equalsIgnoreCase(traceFormat)) {
                // Tempo API endpoint
                url = traceEndpoint + "/api/v2/traces/" + traceId.trim();
            } else {
                // Zipkin API endpoint
                url = traceEndpoint + "/api/v2/trace/" + traceId.trim();
            }
            
            log.debug("Requesting trace from URL: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String body = response.getBody();
                if (body == null || body.trim().isEmpty() || "[]".equals(body.trim())) {
                    log.warn("Empty response for trace: {}", traceId);
                    return createNotFoundTrace(traceId);
                }

                // 根据格式解析
                if ("tempo".equalsIgnoreCase(traceFormat)) {
                    return parseTempoTrace(traceId, body);
                } else {
                    return parseZipkinTrace(traceId, body);
                }
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Trace not found: {}", traceId);
                return createNotFoundTrace(traceId);
            } else {
                log.warn("Failed to get trace: {}, status: {}, body: {}",
                        traceId, response.getStatusCode(), response.getBody());
                return createErrorTrace(traceId, "HTTP " + response.getStatusCode());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.warn("HTTP client error getting trace: {}, status: {}, message: {}",
                    traceId, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return createNotFoundTrace(traceId);
            }
            return createErrorTrace(traceId, e.getMessage());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to trace endpoint: {}, error: {}", traceEndpoint, e.getMessage());
            return createErrorTrace(traceId, "Connection failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error getting trace by id: {}", traceId, e);
            return createErrorTrace(traceId, e.getMessage());
        }
    }
    
    /**
     * 解析Tempo/OpenTelemetry格式的trace
     */
    private Map<String, Object> parseTempoTrace(String traceId, String body) throws Exception {
        Map<String, Object> tempoData = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
        
        @SuppressWarnings("unchecked")
        Map<String, Object> traceData = (Map<String, Object>) tempoData.get("trace");
        if (traceData == null) {
            log.warn("No trace data found in Tempo response");
            return createNotFoundTrace(traceId);
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resourceSpans = (List<Map<String, Object>>) traceData.get("resourceSpans");
        if (resourceSpans == null || resourceSpans.isEmpty()) {
            log.warn("No resourceSpans found in Tempo trace");
            return createNotFoundTrace(traceId);
        }
        
        return convertTempoTrace(traceId, resourceSpans);
    }
    
    /**
     * 解析Zipkin格式的trace
     */
    private Map<String, Object> parseZipkinTrace(String traceId, String body) throws Exception {
        List<Map<String, Object>> spans = objectMapper.readValue(
                body,
                new TypeReference<List<Map<String, Object>>>() {}
        );

        if (spans.isEmpty()) {
            log.warn("No spans found for trace: {}", traceId);
            return createNotFoundTrace(traceId);
        }

        log.info("Successfully retrieved {} spans for trace: {}", spans.size(), traceId);
        return convertZipkinTrace(traceId, spans);
    }
    
    /**
     * 转换Tempo/OpenTelemetry格式为统一格式
     */
    private Map<String, Object> convertTempoTrace(String traceId, List<Map<String, Object>> resourceSpans) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("traceId", traceId);
        
        List<Map<String, Object>> allSpans = new ArrayList<>();
        String serviceName = "unknown";
        String operationName = "unknown";
        long startTime = Long.MAX_VALUE;
        long endTime = 0;
        
        for (Map<String, Object> resourceSpan : resourceSpans) {
            // 提取服务名
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) resourceSpan.get("resource");
            if (resource != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attributes = (List<Map<String, Object>>) resource.get("attributes");
                if (attributes != null) {
                    for (Map<String, Object> attr : attributes) {
                        if ("service.name".equals(attr.get("key"))) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> value = (Map<String, Object>) attr.get("value");
                            if (value != null) {
                                serviceName = (String) value.get("stringValue");
                            }
                        }
                    }
                }
            }
            
            // 提取spans
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> scopeSpans = (List<Map<String, Object>>) resourceSpan.get("scopeSpans");
            if (scopeSpans != null) {
                for (Map<String, Object> scopeSpan : scopeSpans) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> spans = (List<Map<String, Object>>) scopeSpan.get("spans");
                    if (spans != null) {
                        for (Map<String, Object> span : spans) {
                            Map<String, Object> convertedSpan = convertTempoSpan(span, serviceName);
                            allSpans.add(convertedSpan);
                            
                            // 更新trace级别的信息
                            Object startTimeObj = convertedSpan.get("startTime");
                            Object endTimeObj = convertedSpan.get("endTime");
                            if (startTimeObj instanceof Long) {
                                startTime = Math.min(startTime, (Long) startTimeObj);
                            }
                            if (endTimeObj instanceof Long) {
                                endTime = Math.max(endTime, (Long) endTimeObj);
                            }
                            
                            // 获取根span的操作名
                            if (convertedSpan.get("parentSpanId") == null) {
                                operationName = (String) convertedSpan.get("operationName");
                            }
                        }
                    }
                }
            }
        }
        
        trace.put("serviceName", serviceName);
        trace.put("operationName", operationName);
        trace.put("startTime", startTime != Long.MAX_VALUE ? startTime / 1000000 : System.currentTimeMillis());
        trace.put("duration", endTime > startTime ? (endTime - startTime) / 1000000 : 0);
        trace.put("spans", allSpans);
        trace.put("spanCount", allSpans.size());
        trace.put("status", "success");
        
        return trace;
    }
    
    /**
     * 转换单个Tempo span
     */
    private Map<String, Object> convertTempoSpan(Map<String, Object> tempoSpan, String serviceName) {
        Map<String, Object> span = new HashMap<>();
        
        // 基本信息
        span.put("spanId", decodeBase64SpanId((String) tempoSpan.get("spanId")));
        span.put("traceId", decodeBase64TraceId((String) tempoSpan.get("traceId")));
        
        String parentSpanId = (String) tempoSpan.get("parentSpanId");
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            span.put("parentSpanId", decodeBase64SpanId(parentSpanId));
        } else {
            span.put("parentSpanId", null);
        }
        
        span.put("operationName", tempoSpan.get("name"));
        span.put("serviceName", serviceName);
        
        // 时间信息（纳秒转毫秒）
        Object startTimeNano = tempoSpan.get("startTimeUnixNano");
        Object endTimeNano = tempoSpan.get("endTimeUnixNano");
        
        if (startTimeNano != null) {
            long startMs = Long.parseLong(startTimeNano.toString()) / 1000000;
            span.put("startTime", startMs);
        }
        
        if (endTimeNano != null && startTimeNano != null) {
            long endMs = Long.parseLong(endTimeNano.toString()) / 1000000;
            long startMs = Long.parseLong(startTimeNano.toString()) / 1000000;
            span.put("duration", endMs - startMs);
            span.put("endTime", endMs);
        }
        
        // Kind
        String kind = (String) tempoSpan.get("kind");
        if (kind != null) {
            span.put("kind", kind.replace("SPAN_KIND_", ""));
        }
        
        // Attributes转为tags
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) tempoSpan.get("attributes");
        if (attributes != null) {
            Map<String, Object> tags = new HashMap<>();
            for (Map<String, Object> attr : attributes) {
                String key = (String) attr.get("key");
                @SuppressWarnings("unchecked")
                Map<String, Object> value = (Map<String, Object>) attr.get("value");
                if (value != null) {
                    Object tagValue = value.get("stringValue");
                    if (tagValue == null) tagValue = value.get("intValue");
                    if (tagValue == null) tagValue = value.get("boolValue");
                    if (tagValue == null) tagValue = value.get("doubleValue");
                    tags.put(key, tagValue);
                }
            }
            span.put("tags", tags);
        }
        
        // Events转为logs
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) tempoSpan.get("events");
        if (events != null) {
            List<Map<String, Object>> logs = new ArrayList<>();
            for (Map<String, Object> event : events) {
                Map<String, Object> log = new HashMap<>();
                Object timeNano = event.get("timeUnixNano");
                if (timeNano != null) {
                    log.put("timestamp", Long.parseLong(timeNano.toString()) / 1000000);
                }
                log.put("value", event.get("name"));
                logs.add(log);
            }
            span.put("logs", logs);
        }
        
        return span;
    }
    
    /**
     * 解码Base64编码的spanId
     */
    private String decodeBase64SpanId(String base64SpanId) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64SpanId);
            StringBuilder hex = new StringBuilder();
            for (byte b : decoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.warn("Failed to decode spanId: {}", base64SpanId);
            return base64SpanId;
        }
    }
    
    /**
     * 解码Base64编码的traceId
     */
    private String decodeBase64TraceId(String base64TraceId) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64TraceId);
            StringBuilder hex = new StringBuilder();
            for (byte b : decoded) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            log.warn("Failed to decode traceId: {}", base64TraceId);
            return base64TraceId;
        }
    }

    @Override
    public Map<String, Object> searchTraces(TraceQueryRequest request) {
        log.info("Searching traces for service: {}", request.getServiceName());
        
        try {
            // 构建Zipkin查询参数
            StringBuilder urlBuilder = new StringBuilder(traceEndpoint + "/api/v2/traces?");
            
            if (request.getServiceName() != null) {
                urlBuilder.append("serviceName=").append(request.getServiceName()).append("&");
            }
            
            if (request.getOperationName() != null) {
                urlBuilder.append("spanName=").append(request.getOperationName()).append("&");
            }
            
            if (request.getMinDuration() != null) {
                urlBuilder.append("minDuration=").append(request.getMinDuration() * 1000).append("&");
            }
            
            if (request.getMaxDuration() != null) {
                urlBuilder.append("maxDuration=").append(request.getMaxDuration() * 1000).append("&");
            }
            
            if (request.getEndTime() != null) {
                urlBuilder.append("endTs=").append(request.getEndTime()).append("&");
            }
            
            // lookback参数（查询时间范围，单位：毫秒）
            long lookback = 3600000; // 默认1小时
            if (request.getStartTime() != null && request.getEndTime() != null) {
                lookback = request.getEndTime() - request.getStartTime();
            }
            urlBuilder.append("lookback=").append(lookback).append("&");
            
            urlBuilder.append("limit=").append(request.getLimit() != null ? request.getLimit() : 100);
            
            String url = urlBuilder.toString();
            log.debug("Querying traces: {}", url);
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 解析Zipkin返回的traces数组
                List<List<Map<String, Object>>> traces = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<List<List<Map<String, Object>>>>() {}
                );
                
                // 转换为统一格式
                List<Map<String, Object>> convertedTraces = traces.stream()
                    .map(this::convertZipkinTraceList)
                    .collect(Collectors.toList());
                
                Map<String, Object> result = new HashMap<>();
                result.put("traces", convertedTraces);
                result.put("total", convertedTraces.size());
                result.put("limit", request.getLimit());
                
                return result;
            } else {
                log.warn("Failed to search traces, status: {}", response.getStatusCode());
                return createEmptySearchResult();
            }
        } catch (Exception e) {
            log.error("Error searching traces", e);
            return createEmptySearchResult();
        }
    }

    @Override
    public List<String> getServices() {
        log.info("Getting services list from trace endpoint");
        
        try {
            String url = traceEndpoint + "/api/v2/services";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<String> services = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<List<String>>() {}
                );
                log.info("Found {} services", services.size());
                return services;
            } else {
                log.warn("Failed to get services, status: {}", response.getStatusCode());
                return getDefaultServices();
            }
        } catch (Exception e) {
            log.error("Error getting services list", e);
            return getDefaultServices();
        }
    }

    @Override
    public Map<String, Object> getTraceStats(String serviceName, int hours) {
        log.info("Getting trace stats for service: {}, hours: {}", serviceName, hours);
        
        try {
            // 计算时间范围
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (hours * 3600000L);
            
            // 查询traces
            TraceQueryRequest request = new TraceQueryRequest();
            request.setServiceName(serviceName);
            request.setStartTime(startTime);
            request.setEndTime(endTime);
            request.setLimit(1000); // 获取更多数据用于统计
            
            Map<String, Object> searchResult = searchTraces(request);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> traces = (List<Map<String, Object>>) searchResult.get("traces");
            
            // 计算统计数据
            int totalTraces = traces.size();
            int errorTraces = 0;
            long totalDuration = 0;
            long maxDuration = 0;
            long minDuration = Long.MAX_VALUE;
            
            for (Map<String, Object> trace : traces) {
                Object durationObj = trace.get("duration");
                if (durationObj != null) {
                    long duration = ((Number) durationObj).longValue();
                    totalDuration += duration;
                    maxDuration = Math.max(maxDuration, duration);
                    minDuration = Math.min(minDuration, duration);
                }
                
                // 检查是否有错误
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> spans = (List<Map<String, Object>>) trace.get("spans");
                if (spans != null) {
                    for (Map<String, Object> span : spans) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> tags = (Map<String, Object>) span.get("tags");
                        if (tags != null && "true".equals(tags.get("error"))) {
                            errorTraces++;
                            break;
                        }
                    }
                }
            }
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("serviceName", serviceName);
            stats.put("hours", hours);
            stats.put("totalTraces", totalTraces);
            stats.put("errorTraces", errorTraces);
            stats.put("errorRate", totalTraces > 0 ? (double) errorTraces / totalTraces : 0.0);
            stats.put("avgDuration", totalTraces > 0 ? totalDuration / totalTraces : 0);
            stats.put("maxDuration", maxDuration > 0 ? maxDuration : 0);
            stats.put("minDuration", minDuration < Long.MAX_VALUE ? minDuration : 0);
            stats.put("startTime", startTime);
            stats.put("endTime", endTime);
            
            return stats;
        } catch (Exception e) {
            log.error("Error getting trace stats", e);
            return createEmptyStats(serviceName, hours);
        }
    }

    /**
     * 转换Zipkin trace格式为统一格式
     */
    private Map<String, Object> convertZipkinTrace(String traceId, List<Map<String, Object>> spans) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("traceId", traceId);
        
        if (!spans.isEmpty()) {
            // 获取根span信息
            Map<String, Object> rootSpan = findRootSpan(spans);
            if (rootSpan != null) {
                trace.put("serviceName", rootSpan.get("localEndpoint") != null ? 
                    ((Map<?, ?>) rootSpan.get("localEndpoint")).get("serviceName") : "unknown");
                trace.put("operationName", rootSpan.get("name"));
                trace.put("startTime", rootSpan.get("timestamp"));
                trace.put("duration", rootSpan.get("duration"));
            }
            
            // 转换spans
            List<Map<String, Object>> convertedSpans = spans.stream()
                .map(this::convertZipkinSpan)
                .collect(Collectors.toList());
            trace.put("spans", convertedSpans);
            trace.put("spanCount", spans.size());
        }
        
        return trace;
    }

    /**
     * 转换Zipkin trace列表
     */
    private Map<String, Object> convertZipkinTraceList(List<Map<String, Object>> spans) {
        if (spans.isEmpty()) {
            return new HashMap<>();
        }

        String traceId = (String) spans.get(0).get("traceId");
        return convertZipkinTrace(traceId, spans);
    }

    /**
     * 转换Zipkin span格式
     */
    private Map<String, Object> convertZipkinSpan(Map<String, Object> zipkinSpan) {
        Map<String, Object> span = new HashMap<>();
        span.put("spanId", zipkinSpan.get("id"));
        span.put("traceId", zipkinSpan.get("traceId"));
        span.put("parentSpanId", zipkinSpan.get("parentId"));
        span.put("operationName", zipkinSpan.get("name"));
        span.put("startTime", zipkinSpan.get("timestamp"));
        span.put("duration", zipkinSpan.get("duration"));
        span.put("kind", zipkinSpan.get("kind"));
        
        // 提取服务信息
        Object localEndpoint = zipkinSpan.get("localEndpoint");
        if (localEndpoint instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> endpoint = (Map<String, Object>) localEndpoint;
            span.put("serviceName", endpoint.get("serviceName"));
        }
        
        // 提取tags
        span.put("tags", zipkinSpan.get("tags"));
        
        // 提取annotations
        span.put("logs", zipkinSpan.get("annotations"));
        
        return span;
    }

    /**
     * 查找根span
     */
    private Map<String, Object> findRootSpan(List<Map<String, Object>> spans) {
        return spans.stream()
            .filter(span -> span.get("parentId") == null)
            .findFirst()
            .orElse(spans.get(0));
    }

    /**
     * 创建空trace
     */
    private Map<String, Object> createEmptyTrace(String traceId) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("traceId", traceId);
        trace.put("serviceName", "unknown");
        trace.put("operationName", "unknown");
        trace.put("startTime", System.currentTimeMillis());
        trace.put("duration", 0);
        trace.put("spans", new ArrayList<>());
        trace.put("spanCount", 0);
        trace.put("status", "empty");
        return trace;
    }

    /**
     * 创建未找到的trace
     */
    private Map<String, Object> createNotFoundTrace(String traceId) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("traceId", traceId);
        trace.put("serviceName", "unknown");
        trace.put("operationName", "unknown");
        trace.put("startTime", System.currentTimeMillis());
        trace.put("duration", 0);
        trace.put("spans", new ArrayList<>());
        trace.put("spanCount", 0);
        trace.put("status", "not_found");
        trace.put("message", "Trace not found in the tracing system");
        return trace;
    }

    /**
     * 创建错误trace
     */
    private Map<String, Object> createErrorTrace(String traceId, String errorMessage) {
        Map<String, Object> trace = new HashMap<>();
        trace.put("traceId", traceId);
        trace.put("serviceName", "unknown");
        trace.put("operationName", "unknown");
        trace.put("startTime", System.currentTimeMillis());
        trace.put("duration", 0);
        trace.put("spans", new ArrayList<>());
        trace.put("spanCount", 0);
        trace.put("status", "error");
        trace.put("message", errorMessage);
        return trace;
    }

    /**
     * 创建空搜索结果
     */
    private Map<String, Object> createEmptySearchResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("traces", new ArrayList<>());
        result.put("total", 0);
        return result;
    }

    /**
     * 创建空统计数据
     */
    private Map<String, Object> createEmptyStats(String serviceName, int hours) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("serviceName", serviceName);
        stats.put("hours", hours);
        stats.put("totalTraces", 0);
        stats.put("errorTraces", 0);
        stats.put("errorRate", 0.0);
        stats.put("avgDuration", 0);
        stats.put("maxDuration", 0);
        stats.put("minDuration", 0);
        return stats;
    }

    /**
     * 获取默认服务列表
     */
    private List<String> getDefaultServices() {
        return Arrays.asList(
            "basebackend-user-api",
            "basebackend-system-api",
            "basebackend-auth-api",
            "basebackend-notification-service",
            "basebackend-observability-service",
            "basebackend-gateway"
        );
    }
}
