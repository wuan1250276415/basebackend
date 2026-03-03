package com.basebackend.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.mall.trade.dto.OrderSubmitRequest;
import com.basebackend.mall.trade.dto.OrderSubmitResponse;
import com.basebackend.mall.trade.entity.MallOrder;
import com.basebackend.mall.trade.entity.MallOrderItem;
import com.basebackend.mall.trade.enums.MallOrderPayStatus;
import com.basebackend.mall.trade.enums.MallOrderStatus;
import com.basebackend.mall.trade.enums.MallPaymentStatus;
import com.basebackend.mall.trade.event.OrderCancelledMessage;
import com.basebackend.mall.trade.event.OrderCreatedMessage;
import com.basebackend.mall.trade.event.OrderItemSnapshot;
import com.basebackend.mall.trade.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.trade.event.PaymentFailedMessage;
import com.basebackend.mall.trade.event.PaymentSucceededMessage;
import com.basebackend.mall.trade.event.TradeEventTopics;
import com.basebackend.mall.trade.mapper.MallOrderItemMapper;
import com.basebackend.mall.trade.mapper.MallOrderMapper;
import com.basebackend.mall.trade.service.TradeService;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 交易服务实现
 */
@Service
public class TradeServiceImpl implements TradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeServiceImpl.class);
    private static final String BUSINESS_IDEMPOTENT_KEY_PREFIX = "mall:trade:biz:idempotent:";

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MessageProducer messageProducer;
    private final IdempotentStore idempotentStore;

    @Value("${mall.business-idempotent.ttl-seconds:86400}")
    private long businessIdempotentTtlSeconds;

    public TradeServiceImpl(MallOrderMapper mallOrderMapper,
                            MallOrderItemMapper mallOrderItemMapper,
                            MessageProducer messageProducer,
                            IdempotentStore idempotentStore) {
        this.mallOrderMapper = mallOrderMapper;
        this.mallOrderItemMapper = mallOrderItemMapper;
        this.messageProducer = messageProducer;
        this.idempotentStore = idempotentStore;
    }

    @Override
    public String ping() {
        return "mall-trade-api alive";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitResponse submitOrder(OrderSubmitRequest request) {
        String orderNo = "TRD" + LocalDateTime.now().format(ORDER_NO_TIME_FORMAT) + request.userId();

        MallOrder mallOrder = new MallOrder();
        mallOrder.setTenantId(0L);
        mallOrder.setOrderNo(orderNo);
        mallOrder.setUserId(request.userId());
        mallOrder.setOrderStatus(MallOrderStatus.CREATED.getCode());
        mallOrder.setTotalAmount(request.payAmount());
        mallOrder.setPayAmount(request.payAmount());
        mallOrder.setPayStatus(MallOrderPayStatus.UNPAID.getCode());
        mallOrder.setRemark("");
        mallOrder.setSubmitTime(LocalDateTime.now());
        mallOrderMapper.insert(mallOrder);

        int totalQuantity = request.items().stream()
                .mapToInt(OrderSubmitRequest.OrderItem::quantity)
                .sum();
        BigDecimal unitPrice = request.payAmount()
                .divide(BigDecimal.valueOf(Math.max(1, totalQuantity)), 2, RoundingMode.HALF_UP);

        List<OrderCreatedMessage.OrderItem> orderItems = new ArrayList<>();
        for (OrderSubmitRequest.OrderItem item : request.items()) {
            MallOrderItem mallOrderItem = new MallOrderItem();
            mallOrderItem.setTenantId(0L);
            mallOrderItem.setOrderId(mallOrder.getId());
            mallOrderItem.setOrderNo(orderNo);
            mallOrderItem.setSkuId(item.skuId());
            mallOrderItem.setSkuName("SKU-" + item.skuId());
            mallOrderItem.setUnitPrice(unitPrice);
            mallOrderItem.setQuantity(item.quantity());
            mallOrderItem.setLineAmount(unitPrice.multiply(BigDecimal.valueOf(item.quantity())));
            mallOrderItemMapper.insert(mallOrderItem);

            orderItems.add(new OrderCreatedMessage.OrderItem(item.skuId(), item.quantity()));
        }

        OrderCreatedMessage payload = new OrderCreatedMessage(
                mallOrder.getId(),
                orderNo,
                request.userId(),
                request.payAmount(),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                orderItems
        );
        publishMessage(TradeEventTopics.ORDER_CREATED, "ORDER_CREATED", payload);

        return new OrderSubmitResponse(
                orderNo,
                MallOrderStatus.fromCode(mallOrder.getOrderStatus()),
                request.payAmount()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaid(PaymentSucceededMessage message) {
        if (message == null || message.orderNo() == null) {
            return;
        }
        if (message.paymentStatus() != null && message.paymentStatus() != MallPaymentStatus.PAY_SUCCESS) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("payment-succeeded", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            MallOrder mallOrder = findOrderByNo(message.orderNo());
            if (mallOrder == null) {
                return;
            }
            if (!MallOrderStatus.CREATED.matches(mallOrder.getOrderStatus())
                    || !MallOrderPayStatus.UNPAID.matches(mallOrder.getPayStatus())) {
                return;
            }

            mallOrder.setPayStatus(MallOrderPayStatus.PAID.getCode());
            mallOrder.setOrderStatus(MallOrderStatus.PAID.getCode());
            mallOrder.setPayTime(LocalDateTime.now());
            mallOrderMapper.updateById(mallOrder);
            LOGGER.info("订单支付成功，更新订单状态，orderNo={}", mallOrder.getOrderNo());
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markOrderPaymentFailed(PaymentFailedMessage message) {
        if (message == null || message.orderNo() == null) {
            return;
        }
        if (message.paymentStatus() != null && message.paymentStatus() != MallPaymentStatus.PAY_FAILED) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("payment-failed", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            MallOrder mallOrder = findOrderByNo(message.orderNo());
            if (mallOrder == null) {
                return;
            }
            if (!MallOrderStatus.CREATED.matches(mallOrder.getOrderStatus())
                    || !MallOrderPayStatus.UNPAID.matches(mallOrder.getPayStatus())) {
                return;
            }

            mallOrder.setPayStatus(MallOrderPayStatus.PAY_FAILED.getCode());
            mallOrder.setOrderStatus(MallOrderStatus.CANCELLED.getCode());
            mallOrder.setCloseTime(LocalDateTime.now());
            mallOrderMapper.updateById(mallOrder);

            List<OrderItemSnapshot> itemSnapshots = queryOrderItems(mallOrder.getOrderNo());
            OrderCancelledMessage payload = new OrderCancelledMessage(
                    mallOrder.getId(),
                    mallOrder.getOrderNo(),
                    message.reason() == null ? "PAYMENT_FAILED" : message.reason(),
                    MallOrderStatus.CANCELLED,
                    MallOrderPayStatus.PAY_FAILED,
                    itemSnapshots
            );
            publishMessage(TradeEventTopics.ORDER_CANCELLED, "ORDER_CANCELLED", payload);
            LOGGER.info("支付失败回滚完成，orderNo={}", mallOrder.getOrderNo());
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrder(String orderNo) {
        MallOrder mallOrder = findOrderByNo(orderNo);
        if (mallOrder == null) {
            return;
        }

        if (!MallOrderStatus.CREATED.matches(mallOrder.getOrderStatus())
                || !MallOrderPayStatus.UNPAID.matches(mallOrder.getPayStatus())) {
            return;
        }

        mallOrder.setOrderStatus(MallOrderStatus.TIMEOUT_CLOSED.getCode());
        mallOrder.setPayStatus(MallOrderPayStatus.CLOSED.getCode());
        mallOrder.setCloseTime(LocalDateTime.now());
        mallOrderMapper.updateById(mallOrder);

        List<OrderItemSnapshot> itemSnapshots = queryOrderItems(mallOrder.getOrderNo());
        OrderTimeoutClosedMessage payload = new OrderTimeoutClosedMessage(
                mallOrder.getId(),
                mallOrder.getOrderNo(),
                "ORDER_TIMEOUT",
                MallOrderStatus.TIMEOUT_CLOSED,
                MallOrderPayStatus.CLOSED,
                itemSnapshots
        );
        publishMessage(TradeEventTopics.ORDER_TIMEOUT_CLOSED, "ORDER_TIMEOUT_CLOSED", payload);
        LOGGER.info("订单超时关闭完成，orderNo={}", mallOrder.getOrderNo());
    }

    private MallOrder findOrderByNo(String orderNo) {
        LambdaQueryWrapper<MallOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallOrder::getOrderNo, orderNo);
        return mallOrderMapper.selectOne(queryWrapper);
    }

    private List<OrderItemSnapshot> queryOrderItems(String orderNo) {
        LambdaQueryWrapper<MallOrderItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallOrderItem::getOrderNo, orderNo);
        List<MallOrderItem> orderItems = mallOrderItemMapper.selectList(queryWrapper);
        return orderItems.stream()
                .map(item -> new OrderItemSnapshot(item.getSkuId(), item.getQuantity()))
                .toList();
    }

    private <T> void publishMessage(String topic, String messageType, T payload) {
        Message<T> message = Message.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .topic(topic)
                .tags(messageType)
                .messageType(messageType)
                .payload(payload)
                .timestamp(LocalDateTime.now())
                .build();
        messageProducer.send(message);
    }

    private String tryAcquireBusinessKey(String scene, String businessNo) {
        if (!StringUtils.hasText(businessNo)) {
            return null;
        }

        String businessKey = BUSINESS_IDEMPOTENT_KEY_PREFIX + scene + ":" + businessNo;
        boolean acquired = idempotentStore.tryAcquire(
                businessKey,
                businessIdempotentTtlSeconds,
                TimeUnit.SECONDS
        );
        if (!acquired) {
            LOGGER.info("命中业务幂等，跳过重复处理，key={}", businessKey);
            return null;
        }
        return businessKey;
    }
}
