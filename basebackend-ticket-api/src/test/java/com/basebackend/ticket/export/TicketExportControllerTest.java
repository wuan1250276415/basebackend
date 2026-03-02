package com.basebackend.ticket.export;

import com.basebackend.common.export.AsyncExportService;
import com.basebackend.common.export.ExportFormat;
import com.basebackend.common.export.ExportManager;
import com.basebackend.common.export.ExportResult;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketExportControllerTest {

    @InjectMocks
    private TicketExportController exportController;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper categoryMapper;

    @Mock
    private ExportManager exportManager;

    @Mock
    private AsyncExportService asyncExportService;

    private Ticket buildTicket(Long id, String ticketNo) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTicketNo(ticketNo);
        t.setTitle("测试工单");
        t.setStatus("OPEN");
        t.setPriority(2);
        t.setCategoryId(1L);
        t.setReporterName("张三");
        t.setAssigneeName("李四");
        t.setCreateTime(LocalDateTime.now());
        return t;
    }

    @Test
    @DisplayName("同步导出 - 应返回Excel文件字节")
    void shouldExportExcel() {
        TicketCategory cat = new TicketCategory();
        cat.setId(1L);
        cat.setName("技术支持");
        when(categoryMapper.selectList(any())).thenReturn(List.of(cat));
        when(ticketMapper.selectList(any())).thenReturn(List.of(buildTicket(1L, "TK-001")));

        ExportResult mockResult = new ExportResult();
        mockResult.setFileName("tickets.xlsx");
        mockResult.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        mockResult.setContent(new byte[]{1, 2, 3});
        when(exportManager.export(anyList(), eq(TicketExportDTO.class), eq(ExportFormat.XLSX)))
                .thenReturn(mockResult);

        ResponseEntity<byte[]> response = exportController.export(null, "excel");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        verify(exportManager).export(anyList(), eq(TicketExportDTO.class), eq(ExportFormat.XLSX));
    }

    @Test
    @DisplayName("同步导出 - CSV格式应使用CSV导出器")
    void shouldExportCsv() {
        when(categoryMapper.selectList(any())).thenReturn(List.of());
        when(ticketMapper.selectList(any())).thenReturn(List.of());

        ExportResult mockResult = new ExportResult();
        mockResult.setFileName("tickets.csv");
        mockResult.setContentType("text/csv");
        mockResult.setContent(new byte[]{});
        when(exportManager.export(anyList(), eq(TicketExportDTO.class), eq(ExportFormat.CSV)))
                .thenReturn(mockResult);

        ResponseEntity<byte[]> response = exportController.export(null, "csv");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(exportManager).export(anyList(), eq(TicketExportDTO.class), eq(ExportFormat.CSV));
    }

    @Test
    @DisplayName("异步导出 - 应返回任务ID")
    void shouldReturnTaskIdForAsyncExport() {
        when(asyncExportService.exportAsync(any(), eq(TicketExportDTO.class), eq(ExportFormat.XLSX)))
                .thenReturn("task-12345");

        var result = exportController.asyncExport(null, "excel");

        assertThat(result.getData()).isEqualTo("task-12345");
    }

    @Test
    @DisplayName("下载 - 任务不存在时应返回404")
    void shouldReturn404WhenTaskNotFound() {
        when(asyncExportService.getExportResult("no-such-task")).thenReturn(null);

        ResponseEntity<byte[]> response = exportController.download("no-such-task");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
