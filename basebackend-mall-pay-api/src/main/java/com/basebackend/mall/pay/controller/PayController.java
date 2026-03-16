package com.basebackend.mall.pay.controller;

import com.basebackend.common.model.Result;
import com.basebackend.mall.pay.dto.PaymentCreateRequest;
import com.basebackend.mall.pay.dto.PaymentCreateResponse;
import com.basebackend.mall.pay.service.PayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 支付控制器
 */
@RestController
@RequestMapping("/api/mall/payments")
@Validated
@Tag(name = "商城支付", description = "商城支付基础接口")
public class PayController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayController.class);

    private final PayService payService;

    public PayController(PayService payService) {
        this.payService = payService;
    }

    /**
     * 支付服务健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/ping")
    @Operation(summary = "支付服务健康检查")
    public Result<String> ping() {
        return Result.success(payService.ping());
    }

    /**
     * 创建支付单
     *
     * @param request 创建支付单请求
     * @return 支付单响应
     */
    @PostMapping("/create")
    @Operation(summary = "创建支付单")
    public Result<PaymentCreateResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        LOGGER.info("创建支付单，orderNo={}, payChannel={}", request.orderNo(), request.payChannel());
        return Result.success("创建支付单成功", payService.createPayment(request));
    }

    /**
     * 模拟支付成功（联调用）
     *
     * @param orderNo 订单号
     * @return 操作结果
     */
    @PostMapping("/mock-success/{orderNo}")
    @Operation(summary = "模拟支付成功")
    public Result<String> mockPaySuccess(@PathVariable String orderNo) {
        payService.mockPaySuccess(orderNo);
        return Result.success("模拟支付成功完成");
    }

    /**
     * 模拟支付失败（联调用）
     *
     * @param orderNo 订单号
     * @param reason  失败原因
     * @return 操作结果
     */
    @PostMapping("/mock-fail/{orderNo}")
    @Operation(summary = "模拟支付失败")
    public Result<String> mockPayFail(@PathVariable String orderNo,
                                      @RequestParam(defaultValue = "MOCK_PAY_FAILED") String reason) {
        payService.mockPayFail(orderNo, reason);
        return Result.success("模拟支付失败完成");
    }
}
