package com.basebackend.mall.product.service.impl;

import com.basebackend.mall.product.entity.MallSku;
import com.basebackend.mall.product.event.MallOrderPayStatus;
import com.basebackend.mall.product.event.MallOrderStatus;
import com.basebackend.mall.product.event.OrderCancelledMessage;
import com.basebackend.mall.product.event.OrderCreatedMessage;
import com.basebackend.mall.product.event.OrderItemSnapshot;
import com.basebackend.mall.product.event.OrderTimeoutClosedMessage;
import com.basebackend.mall.product.event.PaymentSucceededMessage;
import com.basebackend.mall.product.mapper.MallSkuMapper;
import com.basebackend.common.idempotent.store.IdempotentStore;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * ProductServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl 单元测试")
class ProductServiceImplTest {

    @Mock
    private MallSkuMapper mallSkuMapper;

    @Mock
    private IdempotentStore idempotentStore;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("下单预占库存应增加锁定库存")
    void shouldIncreaseLockedStockWhenReserveStock() {
        when(mallSkuMapper.update(isNull(), any())).thenReturn(1);
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);

        OrderCreatedMessage message = new OrderCreatedMessage(
                301L,
                "TRD202603030001",
                10001L,
                new BigDecimal("99.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 3))
        );

        productService.reserveStockForOrder(message);

        verify(mallSkuMapper).update(isNull(), any());
        verify(mallSkuMapper, never()).updateById(any(MallSku.class));
    }

    @Test
    @DisplayName("库存不足时预占应抛出异常")
    void shouldThrowExceptionWhenReserveStockIfInventoryInsufficient() {
        MallSku mallSku = new MallSku();
        mallSku.setId(10001L);
        mallSku.setStockQuantity(5);
        mallSku.setLockQuantity(4);

        when(mallSkuMapper.update(isNull(), any())).thenReturn(0);
        when(mallSkuMapper.selectById(10001L)).thenReturn(mallSku);
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);

        OrderCreatedMessage message = new OrderCreatedMessage(
                302L,
                "TRD202603030002",
                10002L,
                new BigDecimal("99.00"),
                MallOrderStatus.CREATED,
                MallOrderPayStatus.UNPAID,
                List.of(new OrderCreatedMessage.OrderItem(10001L, 2))
        );

        assertThrows(IllegalStateException.class, () -> productService.reserveStockForOrder(message));
        verify(mallSkuMapper).update(isNull(), any());
        verify(mallSkuMapper, never()).updateById(any(MallSku.class));
    }

    @Test
    @DisplayName("支付成功扣减库存应使用原子条件更新")
    void shouldDeductStockAtomicallyWhenOrderPaid() {
        when(mallSkuMapper.update(isNull(), any())).thenReturn(1);
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);

        PaymentSucceededMessage message = new PaymentSucceededMessage(
                "PAY202603030001",
                301L,
                "TRD202603030001",
                new BigDecimal("99.00"),
                com.basebackend.mall.product.event.MallPaymentStatus.PAY_SUCCESS,
                List.of(new PaymentSucceededMessage.PaidItem(10001L, 2))
        );

        productService.deductStockForPaidOrder(message);

        verify(mallSkuMapper).update(isNull(), any());
        verify(mallSkuMapper, never()).updateById(any(MallSku.class));
    }

    @Test
    @DisplayName("订单取消时应释放预占库存")
    void shouldReleaseReservedStockWhenOrderCancelled() {
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(true);
        when(mallSkuMapper.update(isNull(), any())).thenReturn(1);

        OrderCancelledMessage message = new OrderCancelledMessage(
                301L,
                "TRD202603030003",
                "PAY_FAILED",
                MallOrderStatus.CANCELLED,
                MallOrderPayStatus.PAY_FAILED,
                List.of(
                        new OrderItemSnapshot(10001L, 2),
                        new OrderItemSnapshot(10002L, 1)
                )
        );

        productService.releaseReservedStockForCancelledOrder(message);

        ArgumentCaptor<LambdaUpdateWrapper<MallSku>> wrapperCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(mallSkuMapper, times(2)).update(isNull(), wrapperCaptor.capture());
        assertEquals(
                "lock_quantity = GREATEST(COALESCE(lock_quantity, 0) - 2, 0)",
                wrapperCaptor.getAllValues().get(0).getSqlSet()
        );
        assertEquals(
                "lock_quantity = GREATEST(COALESCE(lock_quantity, 0) - 1, 0)",
                wrapperCaptor.getAllValues().get(1).getSqlSet()
        );
        verify(mallSkuMapper, never()).updateById(any(MallSku.class));
    }

    @Test
    @DisplayName("超时关单释放库存未获取幂等锁时应直接跳过")
    void shouldSkipTimeoutReleaseWhenIdempotentLockNotAcquired() {
        when(idempotentStore.tryAcquire(any(), anyLong(), any())).thenReturn(false);

        OrderTimeoutClosedMessage message = new OrderTimeoutClosedMessage(
                301L,
                "TRD202603030004",
                "TIMEOUT",
                MallOrderStatus.TIMEOUT_CLOSED,
                MallOrderPayStatus.CLOSED,
                List.of(new OrderItemSnapshot(10001L, 2))
        );

        productService.releaseReservedStockForTimeoutOrder(message);

        verify(mallSkuMapper, never()).update(isNull(), any());
        verifyNoMoreInteractions(mallSkuMapper);
    }
}
