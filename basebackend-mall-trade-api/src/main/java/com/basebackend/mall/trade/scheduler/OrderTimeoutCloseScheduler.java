package com.basebackend.mall.trade.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.mall.trade.entity.MallOrder;
import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;
import com.basebackend.mall.trade.mapper.MallOrderMapper;
import com.basebackend.mall.trade.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时关单调度器
 */
@Component
public class OrderTimeoutCloseScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTimeoutCloseScheduler.class);

    private final MallOrderMapper mallOrderMapper;
    private final TradeService tradeService;

    @Value("${mall.trade.timeout-close.enabled:true}")
    private boolean enabled;

    @Value("${mall.trade.timeout-close.minutes:15}")
    private long timeoutMinutes;

    public OrderTimeoutCloseScheduler(MallOrderMapper mallOrderMapper,
                                      TradeService tradeService) {
        this.mallOrderMapper = mallOrderMapper;
        this.tradeService = tradeService;
    }

    /**
     * 周期扫描超时未支付订单
     */
    @Scheduled(fixedDelayString = "${mall.trade.timeout-close.fixed-delay-ms:60000}")
    public void closeTimeoutOrders() {
        if (!enabled) {
            return;
        }

        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        LambdaQueryWrapper<MallOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallOrder::getOrderStatus, MallOrderStatus.CREATED.getCode())
                .eq(MallOrder::getPayStatus, MallOrderPayStatus.UNPAID.getCode())
                .le(MallOrder::getSubmitTime, deadline);

        List<MallOrder> timeoutOrders = mallOrderMapper.selectList(queryWrapper);
        if (timeoutOrders.isEmpty()) {
            return;
        }

        LOGGER.info("扫描到超时未支付订单，count={}", timeoutOrders.size());
        for (MallOrder timeoutOrder : timeoutOrders) {
            tradeService.closeTimeoutOrder(timeoutOrder.getOrderNo());
        }
    }
}
