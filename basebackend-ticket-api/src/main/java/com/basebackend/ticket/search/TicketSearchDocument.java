package com.basebackend.ticket.search;

import com.basebackend.search.annotation.Searchable;
import com.basebackend.search.annotation.SearchField;
import com.basebackend.search.model.IndexDefinition.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单搜索文档 - Elasticsearch 索引映射
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Searchable(index = "ticket")
public class TicketSearchDocument {

    @SearchField(type = FieldType.KEYWORD)
    private String id;

    @SearchField(type = FieldType.TEXT, analyzer = "ik_max_word")
    private String title;

    @SearchField(type = FieldType.TEXT, analyzer = "ik_max_word")
    private String description;

    @SearchField(type = FieldType.KEYWORD)
    private String ticketNo;

    @SearchField(type = FieldType.KEYWORD)
    private String status;

    @SearchField(type = FieldType.INTEGER)
    private Integer priority;

    @SearchField(type = FieldType.KEYWORD)
    private String categoryName;

    @SearchField(type = FieldType.KEYWORD)
    private String reporterName;

    @SearchField(type = FieldType.KEYWORD)
    private String assigneeName;

    @SearchField(type = FieldType.TEXT, analyzer = "ik_max_word")
    private String tags;

    @SearchField(type = FieldType.DATE)
    private LocalDateTime createTime;

    @SearchField(type = FieldType.DATE)
    private LocalDateTime updateTime;
}
