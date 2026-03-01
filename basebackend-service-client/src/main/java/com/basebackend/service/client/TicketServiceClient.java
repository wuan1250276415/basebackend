package com.basebackend.service.client;

import com.basebackend.api.model.ticket.TicketBasicDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 工单服务客户端
 * <p>
 * 基于 Spring 6 @HttpExchange 声明式 HTTP 客户端，
 * 用于其他微服务调用工单服务接口。
 * </p>
 */
@HttpExchange("/api/ticket/tickets")
public interface TicketServiceClient {

    @GetExchange("/{id}")
    @Operation(summary = "根据ID获取工单")
    Result<TicketBasicDTO> getById(@PathVariable("id") Long id);

    @GetExchange("/no/{ticketNo}")
    @Operation(summary = "根据工单号获取工单")
    Result<TicketBasicDTO> getByTicketNo(@PathVariable("ticketNo") String ticketNo);
}
