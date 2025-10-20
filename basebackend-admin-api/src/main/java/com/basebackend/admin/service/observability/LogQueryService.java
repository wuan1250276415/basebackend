package com.basebackend.admin.service.observability;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.basebackend.admin.dto.observability.LogQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Loki 日志查询服务
 */
@Slf4j
@Service
public class LogQueryService {

    @Value("${observability.loki.url:http://localhost:3100}")
    private String lokiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 查询日志
     */
    public Map<String, Object> queryLogs(LogQueryRequest request) {
        try {
            String query = buildLogQLQuery(request);
            String url = lokiUrl + "/loki/api/v1/query_range";

            // 使用 UriComponentsBuilder 构建 URL，避免重复编码
            org.springframework.web.util.UriComponentsBuilder builder =
                org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("query", query)
                    .queryParam("limit", request.getLimit());

            if (request.getStartTime() != null) {
                long startNano = request.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000000L;
                builder.queryParam("start", startNano);
            }
            if (request.getEndTime() != null) {
                long endNano = request.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() * 1000000L;
                builder.queryParam("end", endNano);
            }

            URI lokiUri = builder.build().toUri();
            log.info("Querying Loki: {}", lokiUri);

            String response = restTemplate.getForObject(lokiUri, String.class);
            return parseLokiResponse(response);

        } catch (Exception e) {
            log.error("Failed to query logs from Loki: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取日志统计（按级别分组）
     */
    public Map<String, Object> getLogStats(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 统计各级别日志数量
            for (String level : Arrays.asList("INFO", "WARN", "ERROR", "DEBUG")) {
                LogQueryRequest request = new LogQueryRequest();
                request.setLevel(level);
                request.setStartTime(startTime != null ? startTime : LocalDateTime.now().minusHours(1));
                request.setEndTime(endTime != null ? endTime : LocalDateTime.now());
                request.setLimit(0); // 只获取计数

                String query = buildLogQLQuery(request);
                String countQuery = "count_over_time(" + query + "[1h])";

                Object count = queryInstantLog(countQuery);
                stats.put(level.toLowerCase() + "Count", count != null ? count : 0);
            }

        } catch (Exception e) {
            log.error("Failed to get log stats: {}", e.getMessage(), e);
        }

        return stats;
    }

    /**
     * 根据 TraceId 查询关联日志
     */
    public List<Map<String, Object>> queryLogsByTraceId(String traceId) {
        LogQueryRequest request = new LogQueryRequest();
        request.setTraceId(traceId);
        request.setStartTime(LocalDateTime.now().minusHours(24));
        request.setEndTime(LocalDateTime.now());
        request.setLimit(1000);

        Map<String, Object> result = queryLogs(request);

        if (result.containsKey("logs")) {
            return (List<Map<String, Object>>) result.get("logs");
        }

        return Collections.emptyList();
    }

    /**
     * 构建 LogQL 查询语句
     */
    private String buildLogQLQuery(LogQueryRequest request) {
        StringBuilder query = new StringBuilder("{");

        List<String> filters = new ArrayList<>();

        if (request.getApplication() != null && !request.getApplication().isEmpty()) {
            filters.add("application=\"" + request.getApplication() + "\"");
        }

        if (request.getLevel() != null && !request.getLevel().isEmpty()) {
            filters.add("level=\"" + request.getLevel() + "\"");
        }

        if (filters.isEmpty()) {
            query.append("job=\"loki\"");
        } else {
            query.append(String.join(",", filters));
        }

        query.append("}");

        // 添加行过滤
        if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            query.append(" |= `").append(request.getKeyword()).append("`");
        }

        if (request.getTraceId() != null && !request.getTraceId().isEmpty()) {
            query.append(" | json | traceId=\"").append(request.getTraceId()).append("\"");
        }

        return query.toString();
    }

    /**
     * 查询瞬时日志计数
     */
    private Object queryInstantLog(String query) {
        try {
            URI uri = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl(lokiUrl + "/loki/api/v1/query")
                .queryParam("query", query)
                .build()
                .toUri();

            String response = restTemplate.getForObject(uri, String.class);
            JSONObject json = JSON.parseObject(response);

            if ("success".equals(json.getString("status"))) {
                JSONArray result = json.getJSONObject("data").getJSONArray("result");
                if (result != null && !result.isEmpty()) {
                    JSONArray value = result.getJSONObject(0).getJSONArray("value");
                    return value.get(1);
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to query instant log: {}", query, e);
            return null;
        }
    }

    /**
     * 解析 Loki 响应
     */
    private Map<String, Object> parseLokiResponse(String response) {
        JSONObject json = JSON.parseObject(response);

        Map<String, Object> result = new HashMap<>();
        result.put("status", json.getString("status"));

        if ("success".equals(json.getString("status"))) {
            JSONObject data = json.getJSONObject("data");
            result.put("resultType", data.getString("resultType"));

            JSONArray streams = data.getJSONArray("result");
            List<Map<String, Object>> logs = new ArrayList<>();

            if (streams != null) {
                for (int i = 0; i < streams.size(); i++) {
                    JSONObject stream = streams.getJSONObject(i);
                    JSONArray values = stream.getJSONArray("values");

                    if (values != null) {
                        for (int j = 0; j < values.size(); j++) {
                            JSONArray value = values.getJSONArray(j);
                            Map<String, Object> log = new HashMap<>();
                            log.put("timestamp", value.getLong(0));
                            log.put("line", value.getString(1));
                            logs.add(log);
                        }
                    }
                }
            }

            result.put("logs", logs);
            result.put("total", logs.size());
        } else {
            result.put("error", json.getString("error"));
        }

        return result;
    }

    // 移除这个方法，改用 UriComponentsBuilder
}
