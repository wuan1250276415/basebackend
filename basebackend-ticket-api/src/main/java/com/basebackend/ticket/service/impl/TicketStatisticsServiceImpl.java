package com.basebackend.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.dto.*;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工单统计服务实现
 */
@Service
@RequiredArgsConstructor
public class TicketStatisticsServiceImpl implements TicketStatisticsService {

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;

    @Override
    @Cacheable(value = "ticket:stats", key = "'overview'")
    public TicketOverviewVO overview() {
        long total = ticketMapper.selectCount(null);

        return new TicketOverviewVO(
                total,
                countByStatusName(TicketStatus.OPEN),
                countByStatusName(TicketStatus.IN_PROGRESS),
                countByStatusName(TicketStatus.PENDING_APPROVAL),
                countByStatusName(TicketStatus.RESOLVED),
                countByStatusName(TicketStatus.CLOSED),
                countSlaBreached()
        );
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'byCategory'")
    public Map<String, Long> countByCategory() {
        Map<String, Long> result = new LinkedHashMap<>();
        var categories = categoryMapper.selectList(null);
        for (var category : categories) {
            LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Ticket::getCategoryId, category.getId());
            long count = ticketMapper.selectCount(wrapper);
            if (count > 0) {
                result.put(category.getName(), count);
            }
        }
        return result;
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'byStatus'")
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            long count = countByStatusName(status);
            result.put(status.name(), count);
        }
        return result;
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'trend:' + #days")
    public List<TrendPointVO> getTrend(int days) {
        List<TrendPointVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);

            long opened = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                    .between(Ticket::getCreateTime, start, end));

            long resolved = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                    .between(Ticket::getResolvedAt, start, end));

            long closed = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                    .between(Ticket::getClosedAt, start, end));

            trend.add(new TrendPointVO(date, opened, resolved, closed));
        }
        return trend;
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'resolutionTime'")
    public ResolutionTimeVO getResolutionTimeStats() {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Ticket::getResolvedAt)
                .isNotNull(Ticket::getCreateTime);
        List<Ticket> resolvedTickets = ticketMapper.selectList(wrapper);

        if (resolvedTickets.isEmpty()) {
            return new ResolutionTimeVO(0, 0, 0);
        }

        List<Double> hours = resolvedTickets.stream()
                .map(t -> Duration.between(t.getCreateTime(), t.getResolvedAt()).toMinutes() / 60.0)
                .filter(h -> h >= 0)
                .sorted()
                .toList();

        if (hours.isEmpty()) {
            return new ResolutionTimeVO(0, 0, 0);
        }

        double avg = hours.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double median = hours.get(hours.size() / 2);
        int p90Index = (int) Math.ceil(hours.size() * 0.9) - 1;
        double p90 = hours.get(Math.max(0, Math.min(p90Index, hours.size() - 1)));

        return new ResolutionTimeVO(
                Math.round(avg * 100.0) / 100.0,
                Math.round(median * 100.0) / 100.0,
                Math.round(p90 * 100.0) / 100.0
        );
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'slaCompliance'")
    public SlaComplianceVO getSlaComplianceRate() {
        long total = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .isNotNull(Ticket::getSlaDeadline));
        long breached = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getSlaBreached, 1));

        double complianceRate = total > 0 ? Math.round((1.0 - (double) breached / total) * 10000.0) / 100.0 : 100.0;
        return new SlaComplianceVO(total, breached, complianceRate);
    }

    @Override
    @Cacheable(value = "ticket:stats", key = "'topAssignees:' + #limit")
    public List<AssigneeRankVO> getTopAssignees(int limit) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, TicketStatus.RESOLVED.name())
                .isNotNull(Ticket::getAssigneeId);
        List<Ticket> resolvedTickets = ticketMapper.selectList(wrapper);

        Map<Long, List<Ticket>> grouped = resolvedTickets.stream()
                .collect(Collectors.groupingBy(Ticket::getAssigneeId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    Long assigneeId = entry.getKey();
                    List<Ticket> tickets = entry.getValue();
                    String name = tickets.getFirst().getAssigneeName();
                    long count = tickets.size();
                    double avgHours = tickets.stream()
                            .filter(t -> t.getResolvedAt() != null && t.getCreateTime() != null)
                            .mapToDouble(t -> Duration.between(t.getCreateTime(), t.getResolvedAt()).toMinutes() / 60.0)
                            .average()
                            .orElse(0);
                    return new AssigneeRankVO(assigneeId, name, count, Math.round(avgHours * 100.0) / 100.0);
                })
                .sorted(Comparator.comparingLong(AssigneeRankVO::resolvedCount).reversed())
                .limit(limit)
                .toList();
    }

    private long countByStatusName(TicketStatus status) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getStatus, status.name());
        return ticketMapper.selectCount(wrapper);
    }

    private long countSlaBreached() {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Ticket::getSlaBreached, 1);
        return ticketMapper.selectCount(wrapper);
    }
}
