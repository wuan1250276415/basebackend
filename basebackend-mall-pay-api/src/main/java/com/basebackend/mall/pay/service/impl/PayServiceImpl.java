package com.basebackend.mall.pay.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.basebackend.common.util.JsonUtils;
import com.basebackend.mall.pay.dto.PaymentCreateRequest;
import com.basebackend.mall.pay.dto.PaymentCreateResponse;
import com.basebackend.mall.pay.entity.MallPayment;
import com.basebackend.mall.pay.enums.MallPaymentStatus;
import com.basebackend.mall.pay.event.OrderCreatedMessage;
import com.basebackend.mall.pay.event.MallOrderPayStatus;
import com.basebackend.mall.pay.event.MallOrderStatus;
import com.basebackend.mall.pay.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.pay.event.PayEventTopics;
import com.basebackend.mall.pay.event.PaymentFailedMessage;
import com.basebackend.mall.pay.event.PaymentSucceededMessage;
import com.basebackend.mall.pay.mapper.MallPaymentMapper;
import com.basebackend.mall.pay.service.PayService;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 支付服务实现
 */
@Service
public class PayServiceImpl implements PayService {

    private static final DateTimeFormatter PAY_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String BUSINESS_IDEMPOTENT_KEY_PREFIX = "mall:pay:biz:idempotent:";

    private final MallPaymentMapper mallPaymentMapper;
    private final MessageProducer messageProducer;
    private final IdempotentStore idempotentStore;

    @Value("${mall.business-idempotent.ttl-seconds:86400}")
    private long businessIdempotentTtlSeconds;

    public PayServiceImpl(MallPaymentMapper mallPaymentMapper,
                          MessageProducer messageProducer,
                          IdempotentStore idempotentStore) {
        this.mallPaymentMapper = mallPaymentMapper;
        this.messageProducer = messageProducer;
        this.idempotentStore = idempotentStore;
    }

    @Override
    public String ping() {
        return "mall-pay-api alive";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        String payNo = "PAY" + LocalDateTime.now().format(PAY_NO_TIME_FORMAT) + request.orderId();
        MallPayment mallPayment = new MallPayment();
        mallPayment.setTenantId(0L);
        mallPayment.setPayNo(payNo);
        mallPayment.setOrderId(request.orderId());
        mallPayment.setOrderNo(request.orderNo());
        mallPayment.setPayChannel(request.payChannel());
        mallPayment.setPayStatus(MallPaymentStatus.WAIT_PAY.getCode());
        mallPayment.setPayAmount(request.payAmount());
        mallPayment.setThirdPartyTradeNo("");
        mallPayment.setExpireTime(LocalDateTime.now().plusMinutes(30));
        mallPayment.setOrderItemsJson("[]");
        mallPaymentMapper.insert(mallPayment);

        String payUrl = "/api/mall/payments/" + payNo + "/mock-pay";
        return new PaymentCreateResponse(payNo, MallPaymentStatus.fromCode(mallPayment.getPayStatus()), payUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())) {
            return;
        }
        if (message.orderStatus() != null && message.orderStatus() != MallOrderStatus.CREATED) {
            return;
        }
        if (message.orderPayStatus() != null && message.orderPayStatus() != MallOrderPayStatus.UNPAID) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("order-created", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            MallPayment existingPayment = findByOrderNo(message.orderNo());
            if (existingPayment != null) {
                return;
            }

            String payNo = "PAY" + LocalDateTime.now().format(PAY_NO_TIME_FORMAT) + message.orderId();

            MallPayment mallPayment = new MallPayment();
            mallPayment.setTenantId(0L);
            mallPayment.setPayNo(payNo);
            mallPayment.setOrderId(message.orderId());
            mallPayment.setOrderNo(message.orderNo());
            mallPayment.setPayChannel("MOCK_AUTO");
            mallPayment.setPayStatus(MallPaymentStatus.WAIT_PAY.getCode());
            mallPayment.setPayAmount(message.payAmount());
            mallPayment.setThirdPartyTradeNo("");
            mallPayment.setExpireTime(LocalDateTime.now().plusMinutes(30));
            mallPayment.setOrderItemsJson(JsonUtils.toJsonString(message.items()));
            mallPaymentMapper.insert(mallPayment);
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(String orderNo) {
        MallPayment mallPayment = findByOrderNo(orderNo);
        if (mallPayment == null || !MallPaymentStatus.WAIT_PAY.matches(mallPayment.getPayStatus())) {
            return;
        }

        mallPayment.setPayStatus(MallPaymentStatus.PAY_SUCCESS.getCode());
        mallPayment.setThirdPartyTradeNo("MOCK-" + mallPayment.getPayNo());
        mallPayment.setPaidTime(LocalDateTime.now());
        mallPaymentMapper.updateById(mallPayment);

        PaymentSucceededMessage payload = new PaymentSucceededMessage(
                mallPayment.getPayNo(),
                mallPayment.getOrderId(),
                mallPayment.getOrderNo(),
                mallPayment.getPayAmount(),
                MallPaymentStatus.PAY_SUCCESS,
                readPaidItems(mallPayment)
        );
        publishMessage(PayEventTopics.PAYMENT_SUCCEEDED, "PAYMENT_SUCCEEDED", payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPayFail(String orderNo, String reason) {
        MallPayment mallPayment = findByOrderNo(orderNo);
        if (mallPayment == null || !MallPaymentStatus.WAIT_PAY.matches(mallPayment.getPayStatus())) {
            return;
        }

        mallPayment.setPayStatus(MallPaymentStatus.PAY_FAILED.getCode());
        mallPaymentMapper.updateById(mallPayment);

        PaymentFailedMessage payload = new PaymentFailedMessage(
                mallPayment.getPayNo(),
                mallPayment.getOrderId(),
                mallPayment.getOrderNo(),
                MallPaymentStatus.PAY_FAILED,
                StringUtils.hasText(reason) ? reason : "MOCK_PAY_FAILED"
        );
        publishMessage(PayEventTopics.PAYMENT_FAILED, "PAYMENT_FAILED", payload);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeoutClosed(OrderTimeoutClosedMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())) {
            return;
        }
        if (message.orderStatus() != null && message.orderStatus() != MallOrderStatus.TIMEOUT_CLOSED) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("order-timeout-closed", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            MallPayment mallPayment = findByOrderNo(message.orderNo());
            if (mallPayment == null || !MallPaymentStatus.WAIT_PAY.matches(mallPayment.getPayStatus())) {
                return;
            }

            mallPayment.setPayStatus(MallPaymentStatus.CLOSED.getCode());
            mallPaymentMapper.updateById(mallPayment);
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    private MallPayment findByOrderNo(String orderNo) {
        LambdaQueryWrapper<MallPayment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MallPayment::getOrderNo, orderNo);
        return mallPaymentMapper.selectOne(queryWrapper);
    }

    private List<PaymentSucceededMessage.PaidItem> readPaidItems(MallPayment mallPayment) {
        if (!StringUtils.hasText(mallPayment.getOrderItemsJson())) {
            return List.of();
        }
        List<OrderCreatedMessage.OrderItem> orderItems = JsonUtils.parseObject(
                mallPayment.getOrderItemsJson(),
                new TypeReference<List<OrderCreatedMessage.OrderItem>>() {
                });
        if (orderItems == null || orderItems.isEmpty()) {
            return List.of();
        }
        List<PaymentSucceededMessage.PaidItem> paidItems = new ArrayList<>(orderItems.size());
        for (OrderCreatedMessage.OrderItem orderItem : orderItems) {
            paidItems.add(new PaymentSucceededMessage.PaidItem(orderItem.skuId(), orderItem.quantity()));
        }
        return paidItems;
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
            return null;
        }
        return businessKey;
    }
}
