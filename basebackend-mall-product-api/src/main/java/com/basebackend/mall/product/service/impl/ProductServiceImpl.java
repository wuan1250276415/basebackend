package com.basebackend.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.basebackend.mall.product.dto.ProductDetailDTO;
import com.basebackend.mall.product.entity.MallSku;
import com.basebackend.mall.product.event.OrderCancelledMessage;
import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.event.OrderItemSnapshot;
import com.basebackend.mall.product.event.MallOrderPayStatus;
import com.basebackend.mall.product.event.MallOrderStatus;
import com.basebackend.mall.product.event.MallPaymentStatus;
import com.basebackend.mall.product.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.product.event.PaymentSucceededMessage;
import com.basebackend.mall.product.mapper.MallSkuMapper;
import com.basebackend.mall.product.service.ProductService;
import com.basebackend.common.idempotent.store.IdempotentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductServiceImpl.class);
    private static final String BUSINESS_IDEMPOTENT_KEY_PREFIX = "mall:product:biz:idempotent:";

    private static final Map<Long, ProductDetailDTO> MOCK_PRODUCT_MAP = Map.of(
            10001L, new ProductDetailDTO(
                    10001L,
                    20001L,
                    "SKU-10001",
                    "BaseBackend 企业级网关实战",
                    new BigDecimal("99.00"),
                    500,
                    true
            ),
            10002L, new ProductDetailDTO(
                    10002L,
                    20002L,
                    "SKU-10002",
                    "分布式缓存与一致性实践",
                    new BigDecimal("129.00"),
                    300,
                    true
            )
    );

    private final MallSkuMapper mallSkuMapper;
    private final IdempotentStore idempotentStore;

    @Value("${mall.business-idempotent.ttl-seconds:86400}")
    private long businessIdempotentTtlSeconds;

    public ProductServiceImpl(MallSkuMapper mallSkuMapper, IdempotentStore idempotentStore) {
        this.mallSkuMapper = mallSkuMapper;
        this.idempotentStore = idempotentStore;
    }

    @Override
    public String ping() {
        return "mall-product-api alive";
    }

    @Override
    public ProductDetailDTO getProductBySkuId(Long skuId) {
        MallSku mallSku = mallSkuMapper.selectById(skuId);
        if (mallSku != null) {
            int availableStock = Math.max(0,
                    safeInt(mallSku.getStockQuantity()) - safeInt(mallSku.getLockQuantity()));
            return new ProductDetailDTO(
                    mallSku.getId(),
                    mallSku.getSpuId(),
                    mallSku.getSkuCode(),
                    mallSku.getSkuName(),
                    mallSku.getSalePrice(),
                    availableStock,
                    safeInt(mallSku.getSaleStatus()) == 1
            );
        }

        ProductDetailDTO productDetailDTO = MOCK_PRODUCT_MAP.get(skuId);
        if (productDetailDTO != null) {
            return productDetailDTO;
        }
        return new ProductDetailDTO(
                skuId,
                0L,
                "SKU-" + skuId,
                "演示商品-" + skuId,
                new BigDecimal("0.00"),
                0,
                false
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveStockForOrder(OrderCreatedMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())
                || message.items() == null || message.items().isEmpty()) {
            return;
        }
        if (message.orderStatus() != null && message.orderStatus() != MallOrderStatus.CREATED) {
            return;
        }
        if (message.orderPayStatus() != null && message.orderPayStatus() != MallOrderPayStatus.UNPAID) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("stock-reserve", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            for (OrderCreatedMessage.OrderItem item : message.items()) {
                reserveStockAtomically(item.skuId(), item.quantity());
            }
            LOGGER.info("下单预占库存完成，orderNo={}, itemCount={}", message.orderNo(), message.items().size());
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservedStockForCancelledOrder(OrderCancelledMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())) {
            return;
        }
        if (message.orderStatus() != null && message.orderStatus() != MallOrderStatus.CANCELLED) {
            return;
        }
        String businessKey = tryAcquireBusinessKey("stock-release", message.orderNo());
        if (businessKey == null) {
            return;
        }
        try {
            releaseReservedStock(message.items(), message.orderNo(), "订单取消释放库存");
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseReservedStockForTimeoutOrder(OrderTimeoutClosedMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())) {
            return;
        }
        if (message.orderStatus() != null && message.orderStatus() != MallOrderStatus.TIMEOUT_CLOSED) {
            return;
        }
        String businessKey = tryAcquireBusinessKey("stock-release", message.orderNo());
        if (businessKey == null) {
            return;
        }
        try {
            releaseReservedStock(message.items(), message.orderNo(), "超时关单释放库存");
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStockForPaidOrder(PaymentSucceededMessage message) {
        if (message == null || !StringUtils.hasText(message.orderNo())
                || message.items() == null || message.items().isEmpty()) {
            LOGGER.warn("支付成功消息缺少商品明细，orderNo={}", message != null ? message.orderNo() : null);
            return;
        }
        if (message.paymentStatus() != null && message.paymentStatus() != MallPaymentStatus.PAY_SUCCESS) {
            return;
        }

        String businessKey = tryAcquireBusinessKey("stock-deduct", message.orderNo());
        if (businessKey == null) {
            return;
        }

        try {
            for (PaymentSucceededMessage.PaidItem item : message.items()) {
                deductStockAtomically(item.skuId(), item.quantity());
            }

            LOGGER.info("支付成功后扣减库存完成，orderNo={}, itemCount={}",
                    message.orderNo(), message.items().size());
        } catch (RuntimeException exception) {
            idempotentStore.release(businessKey);
            throw exception;
        }
    }

    private void releaseReservedStock(Iterable<OrderItemSnapshot> items, String orderNo, String scene) {
        if (items == null) {
            return;
        }

        for (OrderItemSnapshot item : items) {
            mallSkuMapper.update(
                    null,
                    new LambdaUpdateWrapper<MallSku>()
                            .eq(MallSku::getId, item.skuId())
                            .setSql("lock_quantity = GREATEST(COALESCE(lock_quantity, 0) - " + safeInt(item.quantity()) + ", 0)")
            );
        }
        LOGGER.info("{}完成，orderNo={}", scene, orderNo);
    }

    private MallSku requireSku(Long skuId) {
        MallSku mallSku = mallSkuMapper.selectById(skuId);
        if (mallSku == null) {
            throw new IllegalStateException("SKU不存在，skuId=" + skuId);
        }
        return mallSku;
    }

    private void reserveStockAtomically(Long skuId, Integer quantity) {
        int affectedRows = mallSkuMapper.update(
                null,
                new LambdaUpdateWrapper<MallSku>()
                        .eq(MallSku::getId, skuId)
                        .apply("(COALESCE(stock_quantity, 0) - COALESCE(lock_quantity, 0)) >= {0}", quantity)
                        .setSql("lock_quantity = COALESCE(lock_quantity, 0) + " + quantity)
        );
        if (affectedRows > 0) {
            return;
        }

        MallSku mallSku = requireSku(skuId);
        int availableStock = safeInt(mallSku.getStockQuantity()) - safeInt(mallSku.getLockQuantity());
        if (availableStock < quantity) {
            throw new IllegalStateException("库存不足，无法预占，skuId=" + skuId);
        }
        throw new IllegalStateException("库存预占失败，skuId=" + skuId);
    }

    private void deductStockAtomically(Long skuId, Integer quantity) {
        int affectedRows = mallSkuMapper.update(
                null,
                new LambdaUpdateWrapper<MallSku>()
                        .eq(MallSku::getId, skuId)
                        .ge(MallSku::getStockQuantity, quantity)
                        .ge(MallSku::getLockQuantity, quantity)
                        .setSql("stock_quantity = COALESCE(stock_quantity, 0) - " + quantity
                                + ", lock_quantity = COALESCE(lock_quantity, 0) - " + quantity)
        );
        if (affectedRows > 0) {
            return;
        }

        MallSku mallSku = requireSku(skuId);
        int currentStock = safeInt(mallSku.getStockQuantity());
        int currentLocked = safeInt(mallSku.getLockQuantity());
        if (currentStock < quantity) {
            throw new IllegalStateException("库存不足，无法扣减，skuId=" + skuId);
        }
        if (currentLocked < quantity) {
            throw new IllegalStateException("锁定库存不足，无法完成扣减，skuId=" + skuId);
        }
        throw new IllegalStateException("库存扣减失败，skuId=" + skuId);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
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
