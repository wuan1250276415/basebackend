package com.basebackend.ticket.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.dto.*;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.entity.TicketCategory;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.impl.TicketStatisticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketStatisticsServiceImplTest {

    @InjectMocks
    private TicketStatisticsServiceImpl statisticsService;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketCategoryMapper categoryMapper;

    @Test
    @DisplayName("getTrend - 应返回指定天数的趋势数据")
    void shouldReturnTrendData() {
        when(ticketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        List<TrendPointVO> trend = statisticsService.getTrend(7);

        assertThat(trend).hasSize(7);
        assertThat(trend.getFirst().date()).isNotNull();
    }

    @Test
    @DisplayName("getResolutionTimeStats - 无已解决工单时返回零值")
    void shouldReturnZeroResolutionTimeWhenEmpty() {
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        ResolutionTimeVO result = statisticsService.getResolutionTimeStats();

        assertThat(result.avgHours()).isEqualTo(0);
        assertThat(result.medianHours()).isEqualTo(0);
        assertThat(result.p90Hours()).isEqualTo(0);
    }

    @Test
    @DisplayName("getResolutionTimeStats - 应正确计算解决时间")
    void shouldCalculateResolutionTime() {
        Ticket t1 = new Ticket();
        t1.setCreateTime(LocalDateTime.now().minusHours(10));
        t1.setResolvedAt(LocalDateTime.now());
        Ticket t2 = new Ticket();
        t2.setCreateTime(LocalDateTime.now().minusHours(5));
        t2.setResolvedAt(LocalDateTime.now());
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t1, t2));

        ResolutionTimeVO result = statisticsService.getResolutionTimeStats();

        assertThat(result.avgHours()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getSlaComplianceRate - 无SLA工单时返回100%")
    void shouldReturn100PercentWhenNoSlaTickets() {
        when(ticketMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        SlaComplianceVO result = statisticsService.getSlaComplianceRate();

        assertThat(result.complianceRate()).isEqualTo(100.0);
    }
}
