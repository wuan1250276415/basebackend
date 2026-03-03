package com.basebackend.mall.trade.controller;

import com.basebackend.common.model.Result;
import com.basebackend.mall.trade.dto.OrderSubmitRequest;
import com.basebackend.mall.trade.dto.OrderSubmitResponse;
import com.basebackend.mall.trade.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交易控制器
 */
@RestController
@RequestMapping("/api/mall/trades")
@Validated
@Tag(name = "商城交易", description = "商城交易基础接口")
public class TradeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeController.class);

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * 交易服务健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/ping")
    @Operation(summary = "交易服务健康检查")
    public Result<String> ping() {
        return Result.success(tradeService.ping());
    }

    /**
     * 提交订单
     *
     * @param request 下单请求
     * @return 下单结果
     */
    @PostMapping("/orders/submit")
    @Operation(summary = "提交订单")
    public Result<OrderSubmitResponse> submitOrder(@Valid @RequestBody OrderSubmitRequest request) {
        LOGGER.info("收到下单请求，userId={}, itemCount={}", request.userId(), request.items().size());
        return Result.success("下单成功", tradeService.submitOrder(request));
    }

    /**
     * 手工触发超时关单（联调用）
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PostMapping("/orders/{orderNo}/timeout-close")
    @Operation(summary = "手工触发超时关单")
    public Result<String> timeoutClose(@PathVariable String orderNo) {
        tradeService.closeTimeoutOrder(orderNo);
        return Result.success("超时关单处理完成");
    }
}
