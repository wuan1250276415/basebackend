package com.basebackend.ticket.export;

import com.basebackend.common.export.ExportField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工单导出 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketExportDTO {

    @ExportField(label = "工单编号", order = 1, width = 20)
    private String ticketNo;

    @ExportField(label = "标题", order = 2, width = 40)
    private String title;

    @ExportField(label = "状态", order = 3, width = 12)
    private String status;

    @ExportField(label = "优先级", order = 4, width = 10)
    private String priority;

    @ExportField(label = "分类", order = 5, width = 15)
    private String categoryName;

    @ExportField(label = "处理人", order = 6, width = 12)
    private String assigneeName;

    @ExportField(label = "提交人", order = 7, width = 12)
    private String reporterName;

    @ExportField(label = "创建时间", order = 8, width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ExportField(label = "解决时间", order = 9, width = 20, format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    @ExportField(label = "SLA违约", order = 10, width = 10)
    private String slaBreached;
}
