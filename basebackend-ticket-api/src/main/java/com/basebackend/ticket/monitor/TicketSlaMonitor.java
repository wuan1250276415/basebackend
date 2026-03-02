package com.basebackend.ticket.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 工单 SLA 违约监控器
 * <p>定时检查 SLA 截止时间已过且未标记违约的工单</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSlaMonitor {

    private final TicketMapper ticketMapper;

    private static final Set<String> TERMINAL_STATUSES = Set.of(
            TicketStatus.RESOLVED.name(),
            TicketStatus.CLOSED.name()
    );

    /**
     * 每5分钟检查一次 SLA 违约
     */
    @Scheduled(fixedRate = 300_000)
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(Ticket::getSlaDeadline, now)
                .eq(Ticket::getSlaBreached, 0)
                .notIn(Ticket::getStatus, TERMINAL_STATUSES)
                .isNotNull(Ticket::getSlaDeadline);

        List<Ticket> breachedTickets = ticketMapper.selectList(wrapper);
        if (breachedTickets.isEmpty()) {
            return;
        }

        log.warn("检测到 {} 个工单 SLA 违约", breachedTickets.size());

        for (Ticket ticket : breachedTickets) {
            LambdaUpdateWrapper<Ticket> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Ticket::getId, ticket.getId())
                    .set(Ticket::getSlaBreached, 1);
            ticketMapper.update(null, updateWrapper);

            log.warn("SLA 违约: ticketNo={}, slaDeadline={}, status={}",
                    ticket.getTicketNo(), ticket.getSlaDeadline(), ticket.getStatus());
        }
    }
}
