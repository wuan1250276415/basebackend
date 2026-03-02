package com.basebackend.ticket.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketSlaMonitorTest {

    @InjectMocks
    private TicketSlaMonitor slaMonitor;

    @Mock
    private TicketMapper ticketMapper;

    private Ticket buildBreachedTicket(Long id, String ticketNo) {
        Ticket t = new Ticket();
        t.setId(id);
        t.setTicketNo(ticketNo);
        t.setStatus("OPEN");
        t.setSlaDeadline(LocalDateTime.now().minusHours(1));
        t.setSlaBreached(0);
        return t;
    }

    @Test
    @DisplayName("应检测并标记SLA违约工单")
    void shouldDetectAndMarkSlaBreaches() {
        Ticket t1 = buildBreachedTicket(1L, "TK-001");
        Ticket t2 = buildBreachedTicket(2L, "TK-002");
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t1, t2));
        when(ticketMapper.update(any(), any())).thenReturn(1);

        slaMonitor.checkSlaBreaches();

        verify(ticketMapper, times(2)).update(any(), any());
    }

    @Test
    @DisplayName("无违约工单时不应执行更新")
    void shouldDoNothingWhenNoBreaches() {
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        slaMonitor.checkSlaBreaches();

        verify(ticketMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("应只查询未标记违约且非终态的工单")
    void shouldQueryCorrectConditions() {
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        slaMonitor.checkSlaBreaches();

        verify(ticketMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("单个工单违约时应正确标记")
    void shouldMarkSingleBreach() {
        Ticket t = buildBreachedTicket(1L, "TK-001");
        when(ticketMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(t));
        when(ticketMapper.update(any(), any())).thenReturn(1);

        slaMonitor.checkSlaBreaches();

        verify(ticketMapper, times(1)).update(any(), any());
    }
}
