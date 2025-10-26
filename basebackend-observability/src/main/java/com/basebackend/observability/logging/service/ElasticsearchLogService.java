package com.basebackend.observability.logging.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.basebackend.observability.logging.model.LogEntry;
import com.basebackend.observability.logging.model.LogSearchQuery;
import com.basebackend.observability.logging.model.LogSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch日志搜索服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchLogService {

    private final ElasticsearchClient esClient;

    /**
     * 搜索日志
     */
    public LogSearchResult search(LogSearchQuery query) {
        try {
            long startTime = System.currentTimeMillis();
            
            SearchRequest request = buildSearchRequest(query);
            SearchResponse<LogEntry> response = esClient.search(request, LogEntry.class);
            
            long took = System.currentTimeMillis() - startTime;
            
            return LogSearchResult.builder()
                    .logs(extractLogs(response))
                    .total(response.hits().total().value())
                    .took(took)
                    .build();
                    
        } catch (Exception e) {
            log.error("Elasticsearch search failed", e);
            throw new RuntimeException("日志搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建搜索请求
     */
    private SearchRequest buildSearchRequest(LogSearchQuery query) {
        return SearchRequest.of(s -> s
                .index("logs-*")
                .query(buildQuery(query))
                .from(query.getFrom() != null ? query.getFrom() : 0)
                .size(query.getSize() != null ? query.getSize() : 50)
                .sort(so -> so
                        .field(f -> f
                                .field(query.getSortField() != null ? query.getSortField() : "timestamp")
                                .order("asc".equalsIgnoreCase(query.getSortOrder()) ? 
                                        SortOrder.Asc : SortOrder.Desc)
                        )
                )
                .highlight(h -> h
                        .fields("message", hf -> hf
                                .preTags("<em>")
                                .postTags("</em>")
                        )
                )
        );
    }

    /**
     * 构建查询条件
     */
    private Query buildQuery(LogSearchQuery query) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // 关键词搜索
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            boolBuilder.must(m -> m
                    .multiMatch(mm -> mm
                            .query(query.getKeyword())
                            .fields("message", "exception_message")
                    )
            );
        }

        // 服务过滤
        if (query.getServices() != null && !query.getServices().isEmpty()) {
            boolBuilder.filter(f -> f
                    .terms(t -> t
                            .field("service")
                            .terms(ts -> ts.value(query.getServices().stream()
                                    .map(s -> co.elastic.clients.elasticsearch._types.FieldValue.of(s))
                                    .collect(Collectors.toList())))
                    )
            );
        }

        // 日志级别过滤
        if (query.getLevels() != null && !query.getLevels().isEmpty()) {
            boolBuilder.filter(f -> f
                    .terms(t -> t
                            .field("level")
                            .terms(ts -> ts.value(query.getLevels().stream()
                                    .map(l -> co.elastic.clients.elasticsearch._types.FieldValue.of(l))
                                    .collect(Collectors.toList())))
                    )
            );
        }

        // 时间范围过滤
        if (query.getStartTime() != null && query.getEndTime() != null) {
            boolBuilder.filter(f -> f
                    .range(r -> r
                            .field("timestamp")
                            .gte(co.elastic.clients.json.JsonData.of(query.getStartTime()))
                            .lte(co.elastic.clients.json.JsonData.of(query.getEndTime()))
                    )
            );
        }

        // TraceId过滤
        if (query.getTraceId() != null) {
            boolBuilder.filter(f -> f
                    .term(t -> t
                            .field("trace_id")
                            .value(query.getTraceId())
                    )
            );
        }

        return Query.of(q -> q.bool(boolBuilder.build()));
    }

    /**
     * 提取日志
     */
    private List<LogEntry> extractLogs(SearchResponse<LogEntry> response) {
        List<LogEntry> logs = new ArrayList<>();
        
        for (Hit<LogEntry> hit : response.hits().hits()) {
            LogEntry log = hit.source();
            if (log != null) {
                log.setId(hit.id());
                logs.add(log);
            }
        }
        
        return logs;
    }

    /**
     * 获取日志上下文
     */
    public LogSearchResult getContext(String logId, int before, int after) {
        try {
            // 1. 获取目标日志
            LogEntry target = getLogById(logId);
            if (target == null) {
                throw new RuntimeException("Log not found: " + logId);
            }

            // 2. 查询前N条
            LogSearchQuery beforeQuery = LogSearchQuery.builder()
                    .traceId(target.getTraceId())
                    .endTime(target.getTimestamp())
                    .size(before)
                    .sortField("timestamp")
                    .sortOrder("desc")
                    .build();
            List<LogEntry> beforeLogs = search(beforeQuery).getLogs();

            // 3. 查询后N条
            LogSearchQuery afterQuery = LogSearchQuery.builder()
                    .traceId(target.getTraceId())
                    .startTime(target.getTimestamp())
                    .size(after)
                    .sortField("timestamp")
                    .sortOrder("asc")
                    .build();
            List<LogEntry> afterLogs = search(afterQuery).getLogs();

            // 4. 合并结果
            List<LogEntry> contextLogs = new ArrayList<>();
            contextLogs.addAll(beforeLogs);
            contextLogs.add(target);
            contextLogs.addAll(afterLogs);

            return LogSearchResult.builder()
                    .logs(contextLogs)
                    .total((long) contextLogs.size())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get log context", e);
            throw new RuntimeException("获取日志上下文失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据ID获取日志
     */
    private LogEntry getLogById(String logId) throws Exception {
        return esClient.get(g -> g
                .index("logs-*")
                .id(logId),
                LogEntry.class
        ).source();
    }
}
