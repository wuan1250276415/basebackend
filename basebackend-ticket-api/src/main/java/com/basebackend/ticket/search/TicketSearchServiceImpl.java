package com.basebackend.ticket.search;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.context.TenantContextHolder;
import com.basebackend.search.client.SearchClient;
import com.basebackend.search.model.IndexDefinition;
import com.basebackend.search.model.SearchResult;
import com.basebackend.search.query.SearchQuery;
import com.basebackend.search.query.SearchQuery.Condition;
import com.basebackend.search.query.SearchQuery.SortOrder;
import com.basebackend.ticket.dto.TicketQueryDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单全文搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "basebackend.search.enabled", havingValue = "true", matchIfMissing = false)
public class TicketSearchServiceImpl implements TicketSearchService {

    private static final String INDEX_NAME = "ticket";

    private final SearchClient searchClient;
    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;

    @PostConstruct
    public void ensureIndex() {
        if (searchClient.indexExists(INDEX_NAME)) {
            log.info("工单搜索索引已存在: {}", INDEX_NAME);
            return;
        }
        log.info("创建工单搜索索引: {}", INDEX_NAME);
        IndexDefinition definition = IndexDefinition.builder(INDEX_NAME)
                .textField("title", "ik_max_word")
                .textField("description", "ik_max_word")
                .keywordField("ticketNo")
                .keywordField("status")
                .integerField("priority")
                .keywordField("categoryName")
                .keywordField("reporterName")
                .keywordField("assigneeName")
                .longField("assigneeId")
                .longField("tenantId")
                .textField("tags", "ik_max_word")
                .dateField("createTime")
                .dateField("updateTime")
                .shards(1)
                .replicas(1)
                .defaultAnalyzer("ik_smart")
                .build();
        searchClient.createIndex(definition);
    }

    @Override
    public void indexTicket(Ticket ticket) {
        String categoryName = resolveCategoryName(ticket.getCategoryId());
        TicketSearchDocument doc = TicketSearchDocument.builder()
                .id(String.valueOf(ticket.getId()))
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .ticketNo(ticket.getTicketNo())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .categoryName(categoryName)
                .reporterName(ticket.getReporterName())
                .assigneeName(ticket.getAssigneeName())
                .assigneeId(ticket.getAssigneeId())
                .tenantId(ticket.getTenantId())
                .tags(ticket.getTags())
                .createTime(ticket.getCreateTime())
                .updateTime(ticket.getUpdateTime())
                .build();
        searchClient.index(INDEX_NAME, doc.getId(), doc);
        log.debug("索引工单文档: ticketNo={}", ticket.getTicketNo());
    }

    @Override
    public void removeTicket(Long ticketId) {
        searchClient.delete(INDEX_NAME, String.valueOf(ticketId));
        log.debug("删除工单索引: ticketId={}", ticketId);
    }

    @Override
    public SearchResult<TicketSearchDocument> search(String keyword, TicketQueryDTO filters, int page, int size) {
        SearchQuery.Builder builder = SearchQuery.builder(INDEX_NAME);

        // 关键词搜索
        if (keyword != null && !keyword.isBlank()) {
            builder.must(Condition.match("title", keyword, 2.0f));
            builder.should(Condition.match("description", keyword));
            builder.should(Condition.match("tags", keyword));
            builder.highlight("title", "description");
        }

        // 过滤条件
        if (filters != null) {
            if (filters.getStatus() != null && !filters.getStatus().isBlank()) {
                builder.filter(Condition.term("status", filters.getStatus()));
            }
            if (filters.getPriority() != null) {
                builder.filter(Condition.term("priority", filters.getPriority()));
            }
            if (filters.getAssigneeId() != null) {
                builder.filter(Condition.term("assigneeId", filters.getAssigneeId()));
            }
            if (filters.getStartDate() != null) {
                builder.filter(Condition.range("createTime", filters.getStartDate(), null));
            }
            if (filters.getEndDate() != null) {
                builder.filter(Condition.range("createTime", null, filters.getEndDate()));
            }
        }

        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            builder.filter(Condition.term("tenantId", tenantId));
        }

        builder.page(page, size)
                .sortByScore()
                .sortBy("createTime", SortOrder.DESC);

        return searchClient.search(builder.build(), TicketSearchDocument.class);
    }

    @Override
    public void reindexAll() {
        log.info("开始全量重建工单搜索索引");
        List<Ticket> tickets = ticketMapper.selectList(new LambdaQueryWrapper<>());
        int count = 0;
        for (Ticket ticket : tickets) {
            indexTicket(ticket);
            count++;
        }
        log.info("工单搜索索引重建完成: count={}", count);
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        TicketCategory category = categoryMapper.selectById(categoryId);
        return category != null ? category.getName() : null;
    }
}
