package com.basebackend.service.client.fallback;

import com.basebackend.api.model.ticket.TicketBasicDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.TicketServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 工单服务客户端降级处理
 */
public class TicketServiceClientFallback implements TicketServiceClient {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceClientFallback.class);

    @Override
    public Result<TicketBasicDTO> getById(Long id) {
        log.warn("工单服务不可用, 降级处理 getById: {}", id);
        return Result.error("工单服务暂时不可用");
    }

    @Override
    public Result<TicketBasicDTO> getByTicketNo(String ticketNo) {
        log.warn("工单服务不可用, 降级处理 getByTicketNo: {}", ticketNo);
        return Result.error("工单服务暂时不可用");
    }
}
