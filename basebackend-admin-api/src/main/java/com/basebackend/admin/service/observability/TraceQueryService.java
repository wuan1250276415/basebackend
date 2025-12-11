package com.basebackend.admin.service.observability;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.basebackend.admin.dto.observability.TraceQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.*;

/**
 * Tempo 追踪查询服务
 */
@Slf4j
@Service
public class TraceQueryService {

    @Value("${observability.tempo.query-url:http://localhost:3200}")
    private String tempoUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 根据 TraceId 查询追踪详情
     */
    public Map<String, Object> getTraceById(String traceId) {
        try {
            String url = tempoUrl + "/api/traces/" + traceId;

            log.info("Querying Tempo trace: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            JSONObject trace = JSON.parseObject(response);

            return parseTraceResponse(trace);

        } catch (Exception e) {
            log.error("Failed to get trace by id: {}", traceId, e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 搜索追踪
     */
    public Map<String, Object> searchTraces(TraceQueryRequest request) {
        try {
            String url = tempoUrl + "/api/search";

            Map<String, Object> params = new HashMap<>();

            if (request.getServiceName() != null) {
                params.put("service.name", request.getServiceName());
            }

            if (request.getMinDuration() != null) {
                params.put("minDuration", request.getMinDuration() + "ms");
            }

            if (request.getMaxDuration() != null) {
                params.put("maxDuration", request.getMaxDuration() + "ms");
            }

            if (request.getStartTime() != null) {
                params.put("start", request.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond());
            }

            if (request.getEndTime() != null) {
                params.put("end", request.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond());
            }

            params.put("limit", request.getLimit());

            String queryString = buildQueryString(params);
            String fullUrl = url + "?" + queryString;

            log.info("Searching Tempo traces: {}", fullUrl);

            String response = restTemplate.getForObject(fullUrl, String.class);
            return parseSearchResponse(response);

        } catch (Exception e) {
            log.error("Failed to search traces: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取服务列表
     */
    public List<String> getServices() {
        try {
            String url = tempoUrl + "/api/search/tags";

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = JSON.parseObject(response);
            JSONArray tagNames = json.getJSONArray("tagNames");

            if (tagNames != null && tagNames.contains("service.name")) {
                String valuesUrl = tempoUrl + "/api/search/tag/service.name/values";
                String valuesResponse = restTemplate.getForObject(valuesUrl, String.class);
                JSONObject valuesJson = JSON.parseObject(valuesResponse);
                JSONArray values = valuesJson.getJSONArray("tagValues");

                List<String> services = new ArrayList<>();
                if (values != null) {
                    for (int i = 0; i < values.size(); i++) {
                        services.add(values.getString(i));
                    }
                }
                return services;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to get services: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取追踪统计
     */
    public Map<String, Object> getTraceStats(String serviceName, int hours) {
        Map<String, Object> stats = new HashMap<>();

        try {
            TraceQueryRequest request = new TraceQueryRequest();
            request.setServiceName(serviceName);
            request.setStartTime(java.time.LocalDateTime.now().minusHours(hours));
            request.setEndTime(java.time.LocalDateTime.now());
            request.setLimit(1000);

            Map<String, Object> searchResult = searchTraces(request);

            if (searchResult.containsKey("traces")) {
                Object tracesObj = searchResult.get("traces");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> traces = (List<Map<String, Object>>) tracesObj;

                stats.put("totalTraces", traces.size());

                // 计算平均持续时间
                long totalDuration = traces.stream()
                    .mapToLong(this::extractDurationMs)
                    .sum();
                stats.put("avgDuration", traces.isEmpty() ? 0 : totalDuration / traces.size());

                // 慢追踪数量（>1秒）
                long slowTraces = traces.stream()
                    .filter(t -> extractDurationMs(t) > 1000)
                    .count();
                stats.put("slowTraces", slowTraces);
            }

        } catch (Exception e) {
            log.error("Failed to get trace stats: {}", e.getMessage(), e);
        }

        return stats;
    }

    /**
     * 解析追踪响应
     */
    private Map<String, Object> parseTraceResponse(JSONObject trace) {
        Map<String, Object> result = new HashMap<>();

        if (trace == null) {
            result.put("error", "Trace not found");
            return result;
        }

        JSONArray batches = trace.getJSONArray("batches");
        if (batches != null && !batches.isEmpty()) {
            JSONObject batch = batches.getJSONObject(0);
            JSONArray spans = batch.getJSONArray("spans");

            result.put("traceId", trace.getString("traceId"));
            result.put("spans", spans);
            result.put("spanCount", spans != null ? spans.size() : 0);
        }

        return result;
    }

    /**
     * 解析搜索响应
     */
    private Map<String, Object> parseSearchResponse(String response) {
        JSONObject json = JSON.parseObject(response);

        Map<String, Object> result = new HashMap<>();
        JSONArray traces = json.getJSONArray("traces");

        List<Map<String, Object>> traceList = new ArrayList<>();
        if (traces != null) {
            for (int i = 0; i < traces.size(); i++) {
                JSONObject trace = traces.getJSONObject(i);
                Map<String, Object> traceInfo = new HashMap<>();
                traceInfo.put("traceId", trace.getString("traceID"));
                traceInfo.put("rootServiceName", trace.getString("rootServiceName"));
                traceInfo.put("rootTraceName", trace.getString("rootTraceName"));
                traceInfo.put("startTimeUnixNano", trace.getLong("startTimeUnixNano"));
                traceInfo.put("durationMs", trace.getLong("durationMs"));
                traceList.add(traceInfo);
            }
        }

        result.put("traces", traceList);
        result.put("total", traceList.size());

        return result;
    }

    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append("&");
            }
            try {
                sb.append(key).append("=").append(java.net.URLEncoder.encode(value.toString(), "UTF-8"));
            } catch (Exception e) {
                log.error("Failed to encode parameter: {}", key, e);
            }
        });
        return sb.toString();
    }

    /**
     * 安全获取持续时间，避免 null 导致的 NPE
     */
    private long extractDurationMs(Map<String, Object> trace) {
        Object value = trace.get("durationMs");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignore) {
                // fall through to default
            }
        }
        return 0L;
    }
}
