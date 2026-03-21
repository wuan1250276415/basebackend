package com.basebackend.mall.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.basebackend.api.model.product.ProductDetailDTO;
import com.basebackend.common.context.UserContextHolder;
import com.basebackend.common.enums.CommonErrorCode;
import com.basebackend.common.exception.BusinessException;
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
import com.basebackend.common.model.Result;
import com.basebackend.messaging.model.Message;
import com.basebackend.messaging.producer.MessageProducer;
import com.basebackend.service.client.ProductServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 交易服务实现
 */
@Service
public class TradeServiceImpl implements TradeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TradeServiceImpl.class);
    private static final String BUSINESS_IDEMPOTENT_KEY_PREFIX = "mall:trade:biz:idempotent:";
    private static final int ORDER_NO_RANDOM_SUFFIX_LENGTH = 8;

    private static final DateTimeFormatter ORDER_NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MessageProducer messageProducer;
    private final IdempotentStore idempotentStore;
    private final ProductServiceClient productServiceClient;

    @Value("${mall.business-idempotent.ttl-seconds:86400}")
    private long businessIdempotentTtlSeconds;

    public TradeServiceImpl(MallOrderMapper mallOrderMapper,
                            MallOrderItemMapper mallOrderItemMapper,
                            MessageProducer messageProducer,
                            IdempotentStore idempotentStore,
                            ProductServiceClient productServiceClient) {
        this.mallOrderMapper = mallOrderMapper;
        this.mallOrderItemMapper = mallOrderItemMapper;
        this.messageProducer = messageProducer;
        this.idempotentStore = idempotentStore;
        this.productServiceClient = productServiceClient;
    }

    @Override
    public String ping() {
        return "mall-trade-api alive";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitResponse submitOrder(OrderSubmitRequest request) {
        Long currentUserId = requireCurrentUserId();
        String orderNo = generateOrderNo(currentUserId);
        List<ResolvedOrderItem> resolvedItems = request.items().stream()
                .map(this::resolveOrderItem)
                .toList();
        BigDecimal totalAmount = resolvedItems.stream()
                .map(ResolvedOrderItem::lineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MallOrder mallOrder = new MallOrder();
        mallOrder.setTenantId(0L);
        mallOrder.setOrderNo(orderNo);
        mallOrder.setUserId(currentUserId);
        mallOrder.setOrderStatus(MallOrderStatus.CREATED.getCode());
        mallOrder.setTotalAmount(totalAmount);
        mallOrder.setPayAmount(totalAmount);
        mallOrder.setPayStatus(MallOrderPayStatus.UNPAID.getCode());
        mallOrder.setRemark("");
        mallOrder.setSubmitTime(LocalDateTime.now());
        mallOrderMapper.insert(mallOrder);

        List<OrderCreatedMessage.OrderItem> orderItems = new ArrayList<>();
        for (ResolvedOrderItem item : resolvedItems) {
            MallOrderItem mallOrderItem = new MallOrderItem();
            mallOrderItem.setTenantId(0L);
            mallOrderItem.setOrderId(mallOrder.getId());
            mallOrderItem.setOrderNo(orderNo);
            mallOrderItem.setSkuId(item.skuId());
            mallOrderItem.setSkuName(item.skuName());
            mallOrderItem.setUnitPrice(item.unitPrice());
            mallOrderItem.setQuantity(item.quantity());
            mallOrderItem.setLineAmount(item.lineAmount());
            mallOrderItemMapper.insert(mallOrderItem);

            orderItems.add(new OrderCreatedMessage.OrderItem(item.skuId(), item.quantity()));
        }

        OrderCreatedMessage payload = new OrderCreatedMessage(
                mallOrder.getId(),
                orderNo,
                currentUserId,
                totalAmount,
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                orderItems
        );
        publishMessage(TradeEventTopics.ORDER_CREATED, "ORDER_CREATED", payload);

        return new OrderSubmitResponse(
                orderNo,
                MallOrderStatus.fromCode(mallOrder.getOrderStatus()),
                totalAmount
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

    private Long requireCurrentUserId() {
        Long currentUserId = UserContextHolder.getUserId();
        if (currentUserId == null) {
            throw BusinessException.unauthorized();
        }
        return currentUserId;
    }

    private ResolvedOrderItem resolveOrderItem(OrderSubmitRequest.OrderItem item) {
        Result<ProductDetailDTO> result = productServiceClient.getProductDetail(item.skuId());
        if (result == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR,
                    "商品服务未返回核价结果，skuId=" + item.skuId());
        }
        if (result.isFailed()) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR,
                    "商品服务核价失败，skuId=" + item.skuId() + "，原因=" + result.getMessage());
        }

        ProductDetailDTO product = result.getData();
        if (product == null || product.salePrice() == null) {
            throw BusinessException.notFound("商品不存在或缺少售价信息，skuId=" + item.skuId());
        }
        if (!Boolean.TRUE.equals(product.onShelf())) {
            throw BusinessException.conflict("商品已下架，skuId=" + item.skuId());
        }

        Integer availableStock = product.availableStock();
        if (availableStock == null || availableStock < item.quantity()) {
            throw BusinessException.conflict("商品库存不足，skuId=" + item.skuId());
        }
        if (product.salePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_SERVICE_ERROR,
                    "商品售价非法，skuId=" + item.skuId());
        }

        BigDecimal lineAmount = product.salePrice().multiply(BigDecimal.valueOf(item.quantity()));
        return new ResolvedOrderItem(
                item.skuId(),
                product.skuName(),
                product.salePrice(),
                item.quantity(),
                lineAmount
        );
    }

    private String generateOrderNo(Long currentUserId) {
        String randomSuffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, ORDER_NO_RANDOM_SUFFIX_LENGTH)
                .toUpperCase(Locale.ROOT);
        return "TRD" + LocalDateTime.now().format(ORDER_NO_TIME_FORMAT) + currentUserId + randomSuffix;
    }

    private record ResolvedOrderItem(
            Long skuId,
            String skuName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal lineAmount) {
    }
}
