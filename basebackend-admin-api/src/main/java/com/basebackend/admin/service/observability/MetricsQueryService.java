package com.basebackend.admin.service.observability;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.basebackend.admin.dto.observability.MetricsQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;
import java.util.*;

/**
 * Prometheus 指标查询服务
 */
@Slf4j
@Service
public class MetricsQueryService {

    @Value("${observability.prometheus.url:http://localhost:9090}")
    private String prometheusUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 查询指标数据
     */
    public Map<String, Object> queryMetrics(MetricsQueryRequest request) {
        try {
            String query = buildPromQLQuery(request);
            String url = prometheusUrl + "/api/v1/query_range";

            Map<String, Object> params = new HashMap<>();
            params.put("query", query);

            if (request.getStartTime() != null) {
                params.put("start", request.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond());
            }
            if (request.getEndTime() != null) {
                params.put("end", request.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond());
            }
            params.put("step", request.getStep() + "s");

            String queryString = buildQueryString(params);
            String fullUrl = url + "?" + queryString;

            log.info("Querying Prometheus: {}", fullUrl);

            String response = restTemplate.getForObject(fullUrl, String.class);
            return parsePrometheusResponse(response);

        } catch (Exception e) {
            log.error("Failed to query metrics from Prometheus: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 获取所有可用指标
     */
    public List<String> getAvailableMetrics() {
        try {
            String url = prometheusUrl + "/api/v1/label/__name__/values";
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = JSON.parseObject(response);
            if ("success".equals(json.getString("status"))) {
                JSONArray data = json.getJSONArray("data");
                List<String> metrics = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    metrics.add(data.getString(i));
                }
                return metrics;
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Failed to get available metrics: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取系统概览指标
     */
    public Map<String, Object> getSystemOverview() {
        Map<String, Object> overview = new HashMap<>();

        try {
            // CPU 使用率
            overview.put("cpuUsage", queryInstantMetric("system_cpu_usage"));

            // 内存使用率
            overview.put("memoryUsage", queryInstantMetric("jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"} * 100"));

            // API 调用总数（最近5分钟）
            overview.put("apiCallsTotal", queryInstantMetric("sum(rate(api_calls_total[5m]))"));

            // API 错误率（最近5分钟）
            overview.put("apiErrorRate", queryInstantMetric("sum(rate(api_calls_total{status=\"error\"}[5m])) / sum(rate(api_calls_total[5m])) * 100"));

            // 平均响应时间（最近5分钟）
            overview.put("avgResponseTime", queryInstantMetric("avg(rate(api_response_time_sum[5m]) / rate(api_response_time_count[5m]))"));

            // 活跃请求数
            overview.put("activeRequests", queryInstantMetric("api_active_requests"));

        } catch (Exception e) {
            log.error("Failed to get system overview: {}", e.getMessage(), e);
        }

        return overview;
    }

    /**
     * 构建 PromQL 查询语句
     */
    private String buildPromQLQuery(MetricsQueryRequest request) {
        StringBuilder query = new StringBuilder(request.getMetricName());

        // 添加标签过滤
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                JSONObject tags = JSON.parseObject(request.getTags());
                if (!tags.isEmpty()) {
                    query.append("{");
                    tags.forEach((key, value) ->
                        query.append(key).append("=\"").append(value).append("\",")
                    );
                    query.deleteCharAt(query.length() - 1); // Remove last comma
                    query.append("}");
                }
            } catch (Exception e) {
                log.warn("Failed to parse tags: {}", request.getTags());
            }
        }

        // 添加聚合函数
        if (request.getAggregation() != null) {
            query.insert(0, request.getAggregation() + "(").append(")");
        }

        return query.toString();
    }

    /**
     * 查询瞬时指标
     */
    private Object queryInstantMetric(String query) {
        try {
            String url = prometheusUrl + "/api/v1/query?query=" +
                        java.net.URLEncoder.encode(query, "UTF-8");

            String response = restTemplate.getForObject(url, String.class);
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
            log.error("Failed to query instant metric: {}", query, e);
            return null;
        }
    }

    /**
     * 解析 Prometheus 响应
     */
    private Map<String, Object> parsePrometheusResponse(String response) {
        JSONObject json = JSON.parseObject(response);

        Map<String, Object> result = new HashMap<>();
        result.put("status", json.getString("status"));

        if ("success".equals(json.getString("status"))) {
            JSONObject data = json.getJSONObject("data");
            result.put("resultType", data.getString("resultType"));
            result.put("result", data.getJSONArray("result"));
        } else {
            result.put("error", json.getString("error"));
        }

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
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }
}
