package com.basebackend.ticket.export;

import com.basebackend.common.export.AsyncExportService;
import com.basebackend.common.export.ExportFormat;
import com.basebackend.common.export.ExportManager;
import com.basebackend.common.export.ExportResult;
import com.basebackend.common.context.TenantContextHolder;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.model.Result;
import com.basebackend.logging.annotation.OperationLog;
import com.basebackend.logging.annotation.OperationLog.BusinessType;
import com.basebackend.security.annotation.RequiresPermission;
import com.basebackend.ticket.dto.TicketQueryDTO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工单导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ticket/export")
@RequiredArgsConstructor
@Tag(name = "工单导出", description = "工单数据导出（CSV/Excel）")
public class TicketExportController {

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;
    private final ExportManager exportManager;
    private final AsyncExportService asyncExportService;
    private final Map<String, ExportTaskOwner> exportTaskOwners = new ConcurrentHashMap<>();

    @GetMapping
    @Operation(summary = "同步导出工单", description = "同步导出工单数据为 CSV 或 Excel")
    @OperationLog(operation = "导出工单", businessType = BusinessType.EXPORT)
    @RequiresPermission("ticket:export")
    public ResponseEntity<byte[]> export(
            TicketQueryDTO filters,
            @Parameter(description = "导出格式: csv/excel") @RequestParam(defaultValue = "excel") String format) {
        log.info("同步导出工单: format={}", format);

        ExportFormat exportFormat = "csv".equalsIgnoreCase(format) ? ExportFormat.CSV : ExportFormat.XLSX;
        List<TicketExportDTO> data = queryExportData(filters);
        ExportResult result = exportManager.export(data, TicketExportDTO.class, exportFormat);

        String encodedFileName = URLEncoder.encode(result.getFileName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }

    @GetMapping("/async")
    @Operation(summary = "异步导出工单", description = "异步导出大量工单数据，返回任务ID")
    @OperationLog(operation = "异步导出工单", businessType = BusinessType.EXPORT)
    @RequiresPermission("ticket:export")
    public Result<String> asyncExport(
            TicketQueryDTO filters,
            @Parameter(description = "导出格式") @RequestParam(defaultValue = "excel") String format) {
        log.info("异步导出工单: format={}", format);

        ExportFormat exportFormat = "csv".equalsIgnoreCase(format) ? ExportFormat.CSV : ExportFormat.XLSX;
        String taskId = asyncExportService.exportAsync(
                () -> queryExportData(filters), TicketExportDTO.class, exportFormat);
        exportTaskOwners.put(taskId, new ExportTaskOwner(UserContextHolder.getUserId(), TenantContextHolder.getTenantId()));
        return Result.success("导出任务已提交", taskId);
    }

    @GetMapping("/status/{taskId}")
    @Operation(summary = "查询导出状态", description = "查询异步导出任务状态")
    @RequiresPermission("ticket:export")
    public Result<Object> exportStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        if (!isCurrentUserTaskOwner(taskId)) {
            return Result.error(CommonErrorCode.NOT_FOUND);
        }
        var status = asyncExportService.getExportStatus(taskId);
        if (status == null) {
            exportTaskOwners.remove(taskId);
            return Result.error(CommonErrorCode.NOT_FOUND);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", status.getStatus().name());
        return Result.success("查询成功", result);
    }

    @GetMapping("/download/{taskId}")
    @Operation(summary = "下载导出文件", description = "下载异步导出的文件")
    @RequiresPermission("ticket:export")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        if (!isCurrentUserTaskOwner(taskId)) {
            return ResponseEntity.notFound().build();
        }
        ExportResult result = asyncExportService.getExportResult(taskId);
        if (result == null) {
            exportTaskOwners.remove(taskId);
            return ResponseEntity.notFound().build();
        }

        String encodedFileName = URLEncoder.encode(result.getFileName(), StandardCharsets.UTF_8);
        exportTaskOwners.remove(taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.parseMediaType(result.getContentType()))
                .body(result.getContent());
    }

    private boolean isCurrentUserTaskOwner(String taskId) {
        ExportTaskOwner owner = exportTaskOwners.get(taskId);
        if (owner == null) {
            return false;
        }
        return Objects.equals(owner.userId(), UserContextHolder.getUserId())
                && Objects.equals(owner.tenantId(), TenantContextHolder.getTenantId());
    }

    private record ExportTaskOwner(Long userId, Long tenantId) {
    }

    private List<TicketExportDTO> queryExportData(TicketQueryDTO filters) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        if (filters != null) {
            if (filters.getStatus() != null && !filters.getStatus().isBlank()) {
                wrapper.eq(Ticket::getStatus, filters.getStatus());
            }
            if (filters.getPriority() != null) {
                wrapper.eq(Ticket::getPriority, filters.getPriority());
            }
            if (filters.getCategoryId() != null) {
                wrapper.eq(Ticket::getCategoryId, filters.getCategoryId());
            }
            if (filters.getAssigneeId() != null) {
                wrapper.eq(Ticket::getAssigneeId, filters.getAssigneeId());
            }
            if (filters.getStartDate() != null) {
                wrapper.ge(Ticket::getCreateTime, filters.getStartDate());
            }
            if (filters.getEndDate() != null) {
                wrapper.le(Ticket::getCreateTime, filters.getEndDate());
            }
        }
        wrapper.orderByDesc(Ticket::getCreateTime);

        // 预加载分类名称映射
        Map<Long, String> categoryMap = new HashMap<>();
        categoryMapper.selectList(null).forEach(c -> categoryMap.put(c.getId(), c.getName()));

        List<Ticket> tickets = ticketMapper.selectList(wrapper);
        return tickets.stream().map(t -> TicketExportDTO.builder()
                .ticketNo(t.getTicketNo())
                .title(t.getTitle())
                .status(t.getStatus())
                .priority(mapPriority(t.getPriority()))
                .categoryName(categoryMap.getOrDefault(t.getCategoryId(), ""))
                .assigneeName(t.getAssigneeName())
                .reporterName(t.getReporterName())
                .createTime(t.getCreateTime())
                .resolvedAt(t.getResolvedAt())
                .slaBreached(t.getSlaBreached() != null && t.getSlaBreached() == 1 ? "是" : "否")
                .build()
        ).toList();
    }

    private String mapPriority(Integer priority) {
        if (priority == null) return "";
        return switch (priority) {
            case 1 -> "紧急";
            case 2 -> "高";
            case 3 -> "中";
            case 4 -> "低";
            default -> "P" + priority;
        };
    }
}
