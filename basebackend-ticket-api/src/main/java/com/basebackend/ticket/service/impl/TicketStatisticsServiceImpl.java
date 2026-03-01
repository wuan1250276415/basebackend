package com.basebackend.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.ticket.dto.TicketOverviewVO;
import com.basebackend.ticket.entity.Ticket;
import com.basebackend.ticket.enums.TicketStatus;
import com.basebackend.ticket.mapper.TicketCategoryMapper;
import com.basebackend.ticket.mapper.TicketMapper;
import com.basebackend.ticket.service.TicketStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工单统计服务实现
 */
@Service
@RequiredArgsConstructor
public class TicketStatisticsServiceImpl implements TicketStatisticsService {

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;

    @Override
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
    public Map<String, Long> countByCategory() {
        // 按分类统计
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
    public Map<String, Long> countByStatus() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            long count = countByStatusName(status);
            result.put(status.name(), count);
        }
        return result;
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
