package com.basebackend.ticket.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 工单指标采集器
 * <p>注册 Micrometer Gauge 指标，实时暴露工单状态分布</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketMetricsCollector {

    private final TicketMapper ticketMapper;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void registerGauges() {
        registerStatusGauge("ticket.count.open", TicketStatus.OPEN);
        registerStatusGauge("ticket.count.in_progress", TicketStatus.IN_PROGRESS);
        registerStatusGauge("ticket.count.pending_approval", TicketStatus.PENDING_APPROVAL);
        registerStatusGauge("ticket.count.resolved", TicketStatus.RESOLVED);
        registerStatusGauge("ticket.count.closed", TicketStatus.CLOSED);

        Gauge.builder("ticket.count.sla_breached", this, c -> c.countSlaBreached())
                .description("SLA violated ticket count")
                .register(meterRegistry);

        Gauge.builder("ticket.count.total", this, c -> c.countTotal())
                .description("Total ticket count")
                .register(meterRegistry);

        log.info("工单 Gauge 指标注册完成");
    }

    private void registerStatusGauge(String name, TicketStatus status) {
        Gauge.builder(name, this, c -> c.countByStatus(status))
                .description("Ticket count with status " + status.name())
                .tag("status", status.name())
                .register(meterRegistry);
    }

    double countByStatus(TicketStatus status) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, status.name());
        return ticketMapper.selectCount(wrapper);
    }

    double countSlaBreached() {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getSlaBreached, 1);
        return ticketMapper.selectCount(wrapper);
    }

    double countTotal() {
        return ticketMapper.selectCount(null);
    }
}
